"""
从项目根目录递归解析 pom.xml，提取所有 <dependency> 条目。
支持：
- 单模块项目（根 pom.xml）
- 多模块项目（根 pom.xml 的 <modules> 列出子模块，递归进入）
- 属性变量替换（${project.version}、${xxx.version} 等）
- dependencyManagement 中的版本继承（含变量版本）
- parent 版本继承
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Set


_NS = "http://maven.apache.org/POM/4.0.0"


def _tag(name: str) -> str:
    return f"{{{_NS}}}{name}"


def _text(elem: ET.Element, tag: str) -> Optional[str]:
    child = elem.find(tag) or elem.find(_tag(tag))
    if child is not None and child.text:
        return child.text.strip()
    return None


def _text_path(elem: ET.Element, *tags: str) -> Optional[str]:
    cur: Optional[ET.Element] = elem
    for tag in tags:
        if cur is None:
            return None
        cur = cur.find(tag) or cur.find(_tag(tag))
    if cur is not None and cur.text:
        return cur.text.strip()
    return None


def _children(elem: ET.Element, tag: str) -> List[ET.Element]:
    return elem.findall(tag) + elem.findall(_tag(tag))


@dataclass
class PomDependency:
    group_id: str
    artifact_id: str
    version: str
    scope: str  # compile / test / provided / runtime / system / import / ""
    parent_group_id: str = ""
    parent_artifact_id: str = ""
    parent_version: str = ""


@dataclass
class _PomData:
    group_id: str = ""
    artifact_id: str = ""
    version: str = ""
    parent_group_id: str = ""
    parent_artifact_id: str = ""
    parent_version: str = ""
    properties: Dict[str, str] = field(default_factory=dict)
    # dep_management 存原始值（可能含 ${...}），在全局合并后统一展开
    dep_management: Dict[str, str] = field(default_factory=dict)
    dependencies: List[PomDependency] = field(default_factory=list)
    modules: List[str] = field(default_factory=list)


def _guess_version_from_props(artifact_id: str, props: Dict[str, str]) -> str:
    """
    当 dependencyManagement 里没有版本时，尝试从 properties 里猜测。
    常见命名模式：elock.version、elock-spring-boot-starter.version 等。
    策略：把 artifactId 里的 - 替换成 . 后逐段缩短匹配。
    """
    # 直接命中
    for key in (
        f"{artifact_id}.version",
        f"{artifact_id.replace('-', '.')}.version",
    ):
        if key in props:
            return props[key]

    # 逐段前缀匹配：elock-spring-boot-starter -> elock.version
    parts = artifact_id.replace("-", ".").split(".")
    for i in range(len(parts), 0, -1):
        candidate = ".".join(parts[:i]) + ".version"
        if candidate in props:
            return props[candidate]

    return ""


def _resolve(value: Optional[str], props: Dict[str, str], max_depth: int = 8) -> str:
    """展开 ${...} 变量，支持多层嵌套，未能解析的占位符保留原样。"""
    if not value:
        return ""
    result = value
    for _ in range(max_depth):
        if "${" not in result:
            break
        prev = result
        for k, v in props.items():
            result = result.replace(f"${{{k}}}", v)
        if result == prev:
            break  # 没有任何替换发生，避免死循环
    return result


def _parse_one(pom_path: Path) -> Optional[_PomData]:
    try:
        tree = ET.parse(str(pom_path))
        root = tree.getroot()
    except Exception:
        return None

    data = _PomData()

    # 基本坐标（groupId / version 可从 parent 继承）
    parent_group_id = _text_path(root, "parent", "groupId") or ""
    parent_artifact_id = _text_path(root, "parent", "artifactId") or ""
    parent_version = _text_path(root, "parent", "version") or ""
    data.parent_group_id = parent_group_id
    data.parent_artifact_id = parent_artifact_id
    data.parent_version = parent_version

    data.group_id = _text(root, "groupId") or parent_group_id
    data.artifact_id = _text(root, "artifactId") or ""
    data.version = _text(root, "version") or parent_version

    # 内置属性（project.* 和 parent.*）
    data.properties["project.version"] = data.version
    data.properties["project.groupId"] = data.group_id
    data.properties["project.artifactId"] = data.artifact_id
    data.properties["project.parent.version"] = parent_version
    data.properties["project.parent.groupId"] = parent_group_id
    data.properties["project.parent.artifactId"] = parent_artifact_id
    # 常见别名
    if parent_version:
        data.properties["parent.version"] = parent_version
        data.properties["revision"] = data.version  # 部分项目用 ${revision}

    # <properties>
    for props_elem in _children(root, "properties"):
        for child in props_elem:
            tag = child.tag.split("}")[-1] if "}" in child.tag else child.tag
            if child.text:
                data.properties[tag] = child.text.strip()

    # <dependencyManagement> — 存原始版本字符串，全局合并后再展开
    for dm in _children(root, "dependencyManagement"):
        for deps in _children(dm, "dependencies"):
            for dep in _children(deps, "dependency"):
                g = _text(dep, "groupId") or ""
                a = _text(dep, "artifactId") or ""
                v = _text(dep, "version") or ""
                scope = _text(dep, "scope") or ""
                # scope=import 的 BOM 也记录，版本可能被其他依赖引用
                if g and a and v:
                    # 先用当前 pom 的 properties 做一次局部展开
                    v_resolved = _resolve(v, data.properties)
                    data.dep_management[f"{g}:{a}"] = v_resolved

    # <dependencies>
    for deps_elem in _children(root, "dependencies"):
        for dep in _children(deps_elem, "dependency"):
            g = _text(dep, "groupId") or ""
            a = _text(dep, "artifactId") or ""
            v = _text(dep, "version") or ""
            scope = _text(dep, "scope") or "compile"
            if g and a:
                data.dependencies.append(PomDependency(
                    group_id=g,
                    artifact_id=a,
                    version=v,
                    scope=scope,
                    parent_group_id=parent_group_id,
                    parent_artifact_id=parent_artifact_id,
                    parent_version=parent_version,
                ))

    # <modules>
    for mods in _children(root, "modules"):
        for mod in _children(mods, "module"):
            if mod.text:
                data.modules.append(mod.text.strip())

    return data


def parse_project_dependencies(repo_root: str) -> List[PomDependency]:
    """
    从 repo_root 递归解析所有 pom.xml，返回去重后的依赖列表。
    版本号做属性替换，从 dependencyManagement 补全缺失版本。
    """
    root_path = Path(repo_root)
    root_pom = root_path / "pom.xml"
    if not root_pom.exists():
        return []

    # BFS 遍历所有模块，根 pom 排第一
    visited: Set[Path] = set()
    queue: List[Path] = [root_pom]
    all_poms: List[_PomData] = []

    while queue:
        pom_path = queue.pop(0)
        pom_path = pom_path.resolve()
        if pom_path in visited:
            continue
        visited.add(pom_path)

        data = _parse_one(pom_path)
        if data is None:
            continue
        all_poms.append(data)

        for mod in data.modules:
            child_pom = pom_path.parent / mod / "pom.xml"
            if child_pom.exists():
                queue.append(child_pom)

    if not all_poms:
        return []

    # 合并所有 pom 的 properties（根 pom 优先，子模块不覆盖根 pom 已有的 key）
    merged_props: Dict[str, str] = {}
    # 先把根 pom 的属性写入
    if all_poms:
        merged_props.update(all_poms[0].properties)
    # 子模块的属性只补充根 pom 没有的 key
    for pom in all_poms[1:]:
        for k, v in pom.properties.items():
            if k not in merged_props:
                merged_props[k] = v

    # 合并 dependencyManagement（根 pom 优先）
    merged_dm: Dict[str, str] = {}
    for pom in reversed(all_poms):  # reversed 使根 pom 最后写入，优先级最高
        merged_dm.update(pom.dep_management)

    # 对 merged_dm 里仍含 ${...} 的版本做全局二次展开
    for key in list(merged_dm.keys()):
        merged_dm[key] = _resolve(merged_dm[key], merged_props)

    # 本项目自身所有模块的坐标集合，用于过滤模块间依赖
    own_modules: Set[str] = set()
    for pom in all_poms:
        g = _resolve(pom.group_id, merged_props)
        a = _resolve(pom.artifact_id, merged_props)
        if g and a:
            own_modules.add(f"{g}:{a}")

    # 根项目坐标（用于 parent 归一化）
    root_group = _resolve(all_poms[0].group_id, merged_props) if all_poms else ""
    root_artifact = _resolve(all_poms[0].artifact_id, merged_props) if all_poms else ""
    root_version = _resolve(all_poms[0].version, merged_props) if all_poms else ""

    # 收集所有依赖，做属性替换和版本补全，跳过本项目自身模块
    seen: Dict[str, PomDependency] = {}  # key = "g:a:scope:pg:pa:pv"
    for pom in all_poms:
        for dep in pom.dependencies:
            g = _resolve(dep.group_id, merged_props)
            a = _resolve(dep.artifact_id, merged_props)
            v = _resolve(dep.version, merged_props)
            scope = dep.scope or "compile"
            pg = _resolve(dep.parent_group_id, merged_props)
            pa = _resolve(dep.parent_artifact_id, merged_props)
            pv = _resolve(dep.parent_version, merged_props)

            # parent 归一化：
            # 若该 parent 不是“本仓库内部模块”，则回退到根项目坐标，
            # 避免把外部企业基座 parent（如 unified-springboot2-parent）混入业务分组。
            if not pa or f"{pg}:{pa}" not in own_modules:
                pg = root_group
                pa = root_artifact
                pv = root_version

            if f"{g}:{a}" in own_modules:
                continue

            # 从 dependencyManagement 补版本（已经过二次展开）
            if not v or "${" in v:
                dm_v = merged_dm.get(f"{g}:{a}", "")
                if dm_v and "${" not in dm_v:
                    v = dm_v
                elif dm_v:
                    v = _resolve(dm_v, merged_props)

            # DM 里也没有，尝试从 properties 猜测（如 elock.version 对应 elock-spring-boot-starter）
            if not v or "${" in v:
                guessed = _guess_version_from_props(a, merged_props)
                if guessed and "${" not in guessed:
                    v = guessed

            # 如果版本仍含未解析的占位符，置空（比显示 ${xxx} 更干净）
            if "${" in v:
                v = ""

            key = f"{g}:{a}:{scope}:{pg}:{pa}:{pv}"
            if key not in seen:
                seen[key] = PomDependency(
                    group_id=g,
                    artifact_id=a,
                    version=v,
                    scope=scope,
                    parent_group_id=pg,
                    parent_artifact_id=pa,
                    parent_version=pv,
                )

    return list(seen.values())

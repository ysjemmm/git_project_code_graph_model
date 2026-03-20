from __future__ import annotations

from typing import Any, Iterable, List, Optional, Sequence, TypeVar

T = TypeVar("T")


class AstTool:
    """
    轻量 AST 工具集（兼容 tree-sitter Node 与 loraxmod ExtractedNode）。
    该仓库的 Java 分析器只依赖这些静态方法。
    """

    @staticmethod
    def get_str(v: Any, default: str = "") -> str:
        if v is None:
            return default
        try:
            s = str(v)
            return s if s else default
        except Exception:
            return default

    @staticmethod
    def node_text(node: Any) -> str:
        if node is None:
            return ""
        # loraxmod ExtractedNode 常见字段
        for attr in ("text", "raw_text", "source", "raw"):
            if hasattr(node, attr):
                try:
                    v = getattr(node, attr)
                    if isinstance(v, bytes):
                        return v.decode("utf-8", errors="replace")
                    if isinstance(v, str):
                        return v
                except Exception:
                    pass
        # tree-sitter Node：有些封装会提供 utf8_text / text 属性
        if hasattr(node, "utf8_text"):
            try:
                v = node.utf8_text  # type: ignore[attr-defined]
                if isinstance(v, bytes):
                    return v.decode("utf-8", errors="replace")
                if isinstance(v, str):
                    return v
            except Exception:
                pass
        # 最后兜底
        try:
            return str(node)
        except Exception:
            return ""

    @staticmethod
    def _iter_children(node: Any) -> List[Any]:
        if node is None:
            return []
        if hasattr(node, "children"):
            try:
                ch = getattr(node, "children")
                if ch is None:
                    return []
                # loraxmod 的某些节点 children 可能不是 list，甚至会是单个 ExtractedNode
                if isinstance(ch, list):
                    return ch
                # 单个子节点
                if hasattr(ch, "node_type") or hasattr(ch, "type"):
                    return [ch]
                # 其他可迭代对象
                try:
                    return list(ch)  # type: ignore[arg-type]
                except TypeError:
                    return []
            except Exception:
                return []
        if hasattr(node, "named_children"):
            try:
                ch = getattr(node, "named_children")
                if isinstance(ch, list):
                    return ch
            except Exception:
                return []
        return []

    @staticmethod
    def iter_children(node: Any) -> List[Any]:
        """公开版 children 迭代（兼容 ExtractedNode children 的异常形态）。"""
        return AstTool._iter_children(node)

    @staticmethod
    def _node_type(node: Any) -> str:
        if node is None:
            return ""
        if hasattr(node, "node_type"):
            try:
                return getattr(node, "node_type") or ""
            except Exception:
                return ""
        if hasattr(node, "type"):
            try:
                return getattr(node, "type") or ""
            except Exception:
                return ""
        return ""

    @staticmethod
    def find_child_by_type(node: Any, node_type: str, deep: bool = False) -> Optional[Any]:
        if node is None or not node_type:
            return None
        for ch in AstTool._iter_children(node):
            if AstTool._node_type(ch) == node_type:
                return ch
            if deep:
                r = AstTool.find_child_by_type(ch, node_type, deep=True)
                if r is not None:
                    return r
        return None

    @staticmethod
    def find_child_by_types(node: Any, node_types: Sequence[str], deep: bool = False) -> List[Any]:
        if node is None:
            return []
        wanted = set([t for t in (node_types or []) if t])
        out: List[Any] = []
        for ch in AstTool._iter_children(node):
            if AstTool._node_type(ch) in wanted:
                out.append(ch)
            if deep:
                out.extend(AstTool.find_child_by_types(ch, node_types, deep=True))
        return out

    @staticmethod
    def join_http_paths(*parts: str) -> str:
        segs: List[str] = []
        for p in parts:
            if not p:
                continue
            s = str(p).strip()
            if not s:
                continue
            if segs:
                s = s.lstrip("/")
            segs.append(s.rstrip("/"))
        return "/".join([s for s in segs if s])


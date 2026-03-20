"""Language adapters for parsing/exporting different languages."""

from __future__ import annotations

from core.language_adapters.protocols import LanguageAdapter, ParseResult


_ADAPTER_FACTORIES: dict[str, callable[[], LanguageAdapter]] = {}
_ALIASES: dict[str, str] = {}


def register_language_adapter(name: str, factory: callable[[], LanguageAdapter], aliases: list[str] | None = None) -> None:
    """
    注册一个语言适配器工厂方法。

    - name: 规范化语言名（会转小写）
    - factory: 无参工厂，返回 LanguageAdapter 实例
    - aliases: 可选别名列表（会映射到 name）
    """
    key = (name or "").strip().lower()
    if not key:
        raise ValueError("language name is required")
    _ADAPTER_FACTORIES[key] = factory
    if aliases:
        for a in aliases:
            ak = (a or "").strip().lower()
            if ak:
                _ALIASES[ak] = key


def get_language_adapter(language: str) -> LanguageAdapter:
    """
    根据语言名称返回对应的 LanguageAdapter 实例。

    说明：
    - 支持通过 register_language_adapter 扩展
    """
    lang = (language or "java").strip().lower()
    lang = _ALIASES.get(lang, lang)
    factory = _ADAPTER_FACTORIES.get(lang)
    if not factory:
        supported = ", ".join(list_supported_languages())
        raise ValueError(f"Unsupported language: {language!r}. Supported: {supported}")
    return factory()


def list_supported_languages(include_aliases: bool = True) -> list[str]:
    """
    返回已注册的语言列表。
    - include_aliases=True 时同时包含别名
    """
    langs = sorted(_ADAPTER_FACTORIES.keys())
    if not include_aliases:
        return langs
    aliases = sorted(_ALIASES.keys())
    return langs + aliases


# 默认注册：java
def _register_defaults() -> None:
    from core.language_adapters.java_adapter import JavaLanguageAdapter

    register_language_adapter(
        "java",
        JavaLanguageAdapter,
        aliases=["jvm-java", "java8", "java11", "java17"],
    )


_register_defaults()


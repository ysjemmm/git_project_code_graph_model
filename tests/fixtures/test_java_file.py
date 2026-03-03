import json
from dataclasses import asdict
from pathlib import Path

from loraxmod import Parser

from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper

if __name__ == '__main__':
    # 创建分析器上下文
    context = AnalyzerContext(
        project_name="TestJavaProject",
        project_path=str(Path(__file__).parent),
        root_project_symbol_id=AnalyzerHelper.generate_symbol_id_for_project("TestJavaProject"),
        parser=Parser("java")
    )

    file_analyzer = JavaFileAnalyzer(
        context=context,
        symbol_table=None,
        auto_resolve_types=True,
        file_path=str(Path(__file__).parent / "TestJavaFile.java")
    )
    java_file_structure = file_analyzer.analyze_file()

    print(json.dumps(asdict(java_file_structure), indent=4))
from __future__ import annotations

from typing import Any, List

from storage.neo4j.java_modules import (
    CommentNodeGraphNode,
    CommentStorageDecision,
    CommentType,
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
    JavadocParseResult,
)


class CommentStorageConfig:
    """注释存储策略配置"""

    SHORT_COMMENT_THRESHOLD = 200  # 短注释阈值（字符数）
    MULTIPLE_COMMENT_THRESHOLD = 3  # 多条注释阈值
    MULTIPLE_COMMENT_MIN_LENGTH = 100  # 多条注释最小总长度


class JavadocParser:
    """Javadoc 解析器"""

    @staticmethod
    def parse(content: str) -> JavadocParseResult:
        result = JavadocParseResult()

        lines = content.split("\n")
        summary_lines: list[str] = []
        in_summary = True

        for line in lines:
            line = line.strip()
            if line.startswith("*"):
                line = line[1:].strip()

            if not line or line in ["/**", "*/"]:
                continue

            if line.startswith("@"):
                in_summary = False

                if line.startswith("@param"):
                    result.params.append(line[6:].strip())
                elif line.startswith("@return"):
                    result.return_desc = line[7:].strip()
                elif line.startswith("@throws") or line.startswith("@exception"):
                    tag_len = 7 if line.startswith("@throws") else 10
                    result.throws.append(line[tag_len:].strip())
                elif line.startswith("@author"):
                    result.author = line[7:].strip()
                elif line.startswith("@version"):
                    result.version = line[8:].strip()
                elif line.startswith("@since"):
                    result.since = line[6:].strip()
                elif line.startswith("@deprecated"):
                    result.deprecated = line[11:].strip()
                elif line.startswith("@see"):
                    result.see.append(line[4:].strip())
            else:
                if in_summary:
                    summary_lines.append(line)

        result.summary = " ".join(summary_lines)
        return result


class JavaCommentProcessingMixin:
    """
    Java 注释/Javadoc 处理能力（从旧 Java exporter 的 mixin 迁移出来）。

    约束：
    - 宿主类需提供：
      - self.nodes_to_create
      - self.created_nodes
      - self.relationships_to_create
    """

    @staticmethod
    def _get_attr(obj: Any, attr: str, default: Any = None) -> Any:
        if obj is None:
            return default
        if isinstance(obj, dict):
            return obj.get(attr, default)
        return getattr(obj, attr, default)

    def _decide_comment_storage_strategy(self, comments: List[Any]) -> CommentStorageDecision:
        if not comments:
            return CommentStorageDecision.STORE_AS_ATTRIBUTE

        has_javadoc = any(
            c.raw_comment.strip().startswith("/**")
            for c in comments
            if hasattr(c, "raw_comment")
        )
        if has_javadoc:
            return CommentStorageDecision.CREATE_JAVADOC_NODE

        total_text = "\n".join(
            [c.raw_comment for c in comments if hasattr(c, "raw_comment")]
        )
        total_length = len(total_text)

        if total_length > CommentStorageConfig.SHORT_COMMENT_THRESHOLD:
            return CommentStorageDecision.CREATE_LONG_COMMENT_NODE

        if (
            len(comments) > CommentStorageConfig.MULTIPLE_COMMENT_THRESHOLD
            and total_length > CommentStorageConfig.MULTIPLE_COMMENT_MIN_LENGTH
        ):
            return CommentStorageDecision.CREATE_LONG_COMMENT_NODE

        return CommentStorageDecision.STORE_AS_ATTRIBUTE

    def _collect_comment_nodes(
        self,
        comments: List[Any],
        parent_symbol_id: str,
        parent_node: Any,
        parent_belong_project: str,
    ) -> None:
        if not comments:
            parent_node.simple_comment = ""
            parent_node.has_detailed_comment = False
            return

        # 不创建 Comment 节点时，仅存于父节点 simple_comment
        if not getattr(self, "include_comment_nodes", False):
            all_text = "\n".join(
                [c.raw_comment for c in comments if hasattr(c, "raw_comment")]
            )
            parent_node.simple_comment = all_text
            parent_node.has_detailed_comment = False
            return

        decision = self._decide_comment_storage_strategy(comments)

        if decision == CommentStorageDecision.STORE_AS_ATTRIBUTE:
            all_text = "\n".join(
                [c.raw_comment for c in comments if hasattr(c, "raw_comment")]
            )
            parent_node.simple_comment = all_text
            parent_node.has_detailed_comment = False
            return

        if decision == CommentStorageDecision.CREATE_JAVADOC_NODE:
            parent_node.has_detailed_comment = True
            self._create_javadoc_nodes(
                comments, parent_symbol_id, parent_node, parent_belong_project
            )
            return

        if decision == CommentStorageDecision.CREATE_LONG_COMMENT_NODE:
            parent_node.has_detailed_comment = True
            self._create_long_comment_node(comments, parent_symbol_id, parent_belong_project)

    def _create_javadoc_nodes(
        self,
        comments: List[Any],
        parent_symbol_id: str,
        parent_node: Any,
        parent_belong_project: str,
    ) -> None:
        javadoc_comments = []
        other_comments = []

        for comment in comments:
            if not hasattr(comment, "raw_comment"):
                continue
            if comment.raw_comment.strip().startswith("/**"):
                javadoc_comments.append(comment)
            else:
                other_comments.append(comment)

        for idx, comment in enumerate(javadoc_comments):
            comment_node = CommentNodeGraphNode()
            comment_node.content = comment.raw_comment
            comment_node.comment_type = CommentType.JAVADOC.value
            comment_node.belong_project = parent_belong_project
            comment_node.char_count = len(comment.raw_comment)
            comment_node.line_count = comment.raw_comment.count("\n") + 1

            javadoc_result = JavadocParser.parse(comment.raw_comment)
            comment_node.javadoc_summary = javadoc_result.summary
            comment_node.javadoc_params = javadoc_result.params
            comment_node.javadoc_return = javadoc_result.return_desc
            comment_node.javadoc_throws = javadoc_result.throws
            comment_node.javadoc_author = javadoc_result.author
            comment_node.javadoc_version = javadoc_result.version
            comment_node.javadoc_since = javadoc_result.since
            comment_node.javadoc_deprecated = javadoc_result.deprecated
            comment_node.javadoc_see = javadoc_result.see

            comment_node.start_line = comment.location.start_line
            comment_node.end_line = comment.location.end_line
            comment_node.start_column = comment.location.start_column
            comment_node.end_column = comment.location.end_column

            comment_node.symbol_id = f"{parent_symbol_id}@javadoc#{idx}"
            comment_node.parent_symbol_id = parent_symbol_id
            comment_node.name = f"javadoc_{idx}"

            self.nodes_to_create[JavaNeo4jNodeType.Comment].append(comment_node)
            self.created_nodes.add(comment_node.symbol_id)
            self.relationships_to_create.append(
                (parent_symbol_id, comment_node.symbol_id, JavaGraphEdgeType.HAS_COMMENT.value)
            )

        if other_comments:
            parent_node.simple_comment = "\n".join([c.raw_comment for c in other_comments])
        else:
            parent_node.simple_comment = ""

    def _create_long_comment_node(
        self,
        comments: List[Any],
        parent_symbol_id: str,
        parent_belong_project: str,
    ) -> None:
        comment_node = CommentNodeGraphNode()
        all_text = "\n---\n".join(
            [c.raw_comment for c in comments if hasattr(c, "raw_comment")]
        )
        comment_node.content = all_text
        comment_node.comment_type = CommentType.LONG_COMMENT.value
        comment_node.belong_project = parent_belong_project
        comment_node.char_count = len(all_text)
        comment_node.line_count = all_text.count("\n") + 1

        first_comment = comments[0]
        last_comment = comments[-1]
        comment_node.start_line = first_comment.location.start_line
        comment_node.end_line = last_comment.location.end_line
        comment_node.start_column = first_comment.location.start_column
        comment_node.end_column = last_comment.location.end_column

        comment_node.symbol_id = f"{parent_symbol_id}@longcomment"
        comment_node.parent_symbol_id = parent_symbol_id
        comment_node.name = "long_comment"

        self.nodes_to_create[JavaNeo4jNodeType.Comment].append(comment_node)
        self.created_nodes.add(comment_node.symbol_id)
        self.relationships_to_create.append(
            (parent_symbol_id, comment_node.symbol_id, JavaGraphEdgeType.HAS_COMMENT.value)
        )


from loraxmod import NodeInterface, ExtractedNode

class AstTool:

    @staticmethod
    def find_child_by_types(node: ExtractedNode, target_types: list[str], **args) -> list[ExtractedNode]:
        
        return list(filter(lambda child: child.node_type in target_types,
                    node.children))

    @staticmethod
    def find_child_by_type(node: ExtractedNode, target_type: str, first: bool = False, **args) -> list[ExtractedNode] | ExtractedNode | None:
        match nodes := AstTool.find_child_by_types(node, [target_type], **args):
            case [single_node]:
                return single_node if first else [single_node]
            case _:
                return nodes

    @staticmethod
    def node_text(node: NodeInterface | ExtractedNode, strip_multiline: bool = False) -> str:
        
        if node is None:
            return ''
        
        # 获取原始文本
        if isinstance(node.text, str):
            text = str(node.text)
        else:
            text = node.text.decode('utf-8')
        
        # 如果需要处理多行文本的缩进
        if strip_multiline:
            lines = text.split('\n')
            
            # 找到所有非空行中最小的缩进
            min_indent = float('inf')
            for line in lines:
                if line.strip():  # 非空行
                    indent = len(line) - len(line.lstrip())
                    min_indent = min(min_indent, indent)
            
            # 如果没有找到任何非空行或最小缩进为 0,直接返回原文本
            if min_indent == float('inf') or min_indent == 0:
                return text
            
            # 从所有行中去掉最小缩进
            processed_lines = []
            for line in lines:
                if line.strip():  # 非空行
                    processed_lines.append(line[min_indent:])
                else:  # 空行
                    processed_lines.append('')
            
            text = '\n'.join(processed_lines)
        else:
            # 默认行为:去掉前后空格
            text = text.strip()
        
        return text

    @staticmethod
    def join_http_paths(base: str, path: str) -> str:
        """正确拼接 HTTP 路径"""
        base = base.rstrip('/')
        path = path.lstrip('/')

        if not path and path.strip() != '':
            return base

        result = f"{base}/{path}"

        # 确保以 / 开头
        if not result.startswith('/'):
            result = '/' + result

        if not result.endswith('/'):
            result += '/'

        return result
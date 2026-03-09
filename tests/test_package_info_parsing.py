#!/usr/bin/env python3
"""
测试 package-info 类和特殊类名的解析
"""
from storage.sqlite.class_name_parser import ClassNameParser


def test_package_info_parsing():
    """测试 package-info 类和特殊类名的解析"""
    parser = ClassNameParser()
    
    test_cases = [
        # (输入路径, 期望的 (fqn, simple_name, package_name, is_anonymous))
        # package-info 类
        (
            "com/example/package-info.class",
            ("com.example.package-info", "package-info", "com.example", False)
        ),
        (
            "com/alibaba/nacos/shaded/io/grpc/netty/package-info.class",
            ("com.alibaba.nacos.shaded.io.grpc.netty.package-info", "package-info", 
             "com.alibaba.nacos.shaded.io.grpc.netty", False)
        ),
        # 普通类
        (
            "com/example/User.class",
            ("com.example.User", "User", "com.example", False)
        ),
        # 内部类
        (
            "com/example/Outer$Inner.class",
            ("com.example.Outer.Inner", "Outer.Inner", "com.example", False)
        ),
        # 匿名类
        (
            "com/example/User$1.class",
            ("com.example.User$1", "User$1", "com.example", True)
        ),
        # 小写内部类
        (
            "com/sun/jna/platform/linux/XAttr$size_t.class",
            ("com.sun.jna.platform.linux.XAttr.size_t", "XAttr.size_t", "com.sun.jna.platform.linux", False)
        ),
        # 以 $ 开头的类名（特殊内部类）
        (
            "com/google/gson/internal/$Gson$Types.class",
            ("com.google.gson.internal.Gson.Types", "Gson.Types", "com.google.gson.internal", False)
        ),
        (
            "com/google/inject/internal/asm/$Attribute$Set.class",
            ("com.google.inject.internal.asm.Attribute.Set", "Attribute.Set", "com.google.inject.internal.asm", False)
        ),
        (
            "com/google/inject/internal/cglib/core/$AbstractClassGenerator$ClassLoaderData.class",
            ("com.google.inject.internal.cglib.core.AbstractClassGenerator.ClassLoaderData", 
             "AbstractClassGenerator.ClassLoaderData", "com.google.inject.internal.cglib.core", False)
        ),
    ]
    
    print("测试 package-info 和特殊类名的解析:")
    print("=" * 80)
    
    all_passed = True
    
    for i, (input_path, expected) in enumerate(test_cases, 1):
        result = parser.parse_class_path(input_path)
        
        if result == expected:
            print(f"✓ 测试 {i} 通过")
            print(f"  输入: {input_path}")
            print(f"  FQN: {result[0]}")
            print(f"  simple_name: {result[1]}")
            print(f"  package_name: {result[2]}")
            print()
        else:
            print(f"✗ 测试 {i} 失败")
            print(f"  输入: {input_path}")
            print(f"  期望: {expected}")
            print(f"  实际: {result}")
            print()
            all_passed = False
    
    if all_passed:
        print("=" * 80)
        print("✓ 所有测试通过！")
    else:
        print("=" * 80)
        print("✗ 部分测试失败")
        raise AssertionError("测试失败")


if __name__ == "__main__":
    test_package_info_parsing()

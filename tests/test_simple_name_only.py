#!/usr/bin/env python3
"""
测试 simple_name 只包含最后一个类名
"""
from storage.sqlite.class_name_parser import ClassNameParser


def test_simple_name_only():
    """测试 simple_name 只包含最后一个类名"""
    parser = ClassNameParser()
    
    test_cases = [
        # (输入路径, 期望的 (fqn, simple_name, package_name, is_anonymous))
        # 普通类
        (
            "com/example/User.class",
            ("com.example.User", "User", "com.example", False)
        ),
        # 一层内部类
        (
            "com/example/Outer$Inner.class",
            ("com.example.Outer.Inner", "Inner", "com.example.Outer", False)
        ),
        # 两层内部类
        (
            "org/bouncycastle/jcajce/provider/symmetric/AES$AESCCMMAC$CCMMac.class",
            ("org.bouncycastle.jcajce.provider.symmetric.AES.AESCCMMAC.CCMMac", 
             "CCMMac", 
             "org.bouncycastle.jcajce.provider.symmetric.AES.AESCCMMAC", 
             False)
        ),
        # 匿名类（保留 $1）
        (
            "com/example/User$1.class",
            ("com.example.User$1", "User$1", "com.example", True)
        ),
        # 内部类的匿名类
        (
            "com/example/Outer$Inner$1.class",
            ("com.example.Outer.Inner$1", "Inner$1", "com.example.Outer", True)
        ),
        # package-info
        (
            "com/example/package-info.class",
            ("com.example.package-info", "package-info", "com.example", False)
        ),
        # 小写内部类
        (
            "com/sun/jna/platform/linux/XAttr$size_t.class",
            ("com.sun.jna.platform.linux.XAttr.size_t", "size_t", "com.sun.jna.platform.linux.XAttr", False)
        ),
        # 以 $ 开头的类
        (
            "com/google/gson/internal/$Gson$Types.class",
            ("com.google.gson.internal.Gson.Types", "Types", "com.google.gson.internal.Gson", False)
        ),
        # 三层嵌套
        (
            "com/example/A$B$C.class",
            ("com.example.A.B.C", "C", "com.example.A.B", False)
        ),
        # $数字+类名（编译器生成的辅助类）
        (
            "com/example/Outer$1ConnectListener.class",
            ("com.example.Outer.ConnectListener", "ConnectListener", "com.example.Outer", False)
        ),
        (
            "com/example/Handler$1LogOnFailure.class",
            ("com.example.Handler.LogOnFailure", "LogOnFailure", "com.example.Handler", False)
        ),
    ]
    
    print("测试 simple_name 只包含最后一个类名:")
    print("=" * 80)
    
    all_passed = True
    
    for i, (input_path, expected) in enumerate(test_cases, 1):
        result = parser.parse_class_path(input_path)
        
        if result == expected:
            print(f"✓ 测试 {i} 通过")
            print(f"  输入: {input_path}")
            print(f"  FQN: {result[0]}")
            print(f"  simple_name: '{result[1]}'")
            print(f"  package: {result[2]}")
            print()
        else:
            print(f"✗ 测试 {i} 失败")
            print(f"  输入: {input_path}")
            print(f"  期望:")
            print(f"    FQN: {expected[0]}")
            print(f"    simple_name: '{expected[1]}'")
            print(f"    package: {expected[2]}")
            print(f"  实际:")
            print(f"    FQN: {result[0]}")
            print(f"    simple_name: '{result[1]}'")
            print(f"    package: {result[2]}")
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
    test_simple_name_only()

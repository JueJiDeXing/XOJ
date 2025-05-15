package com.jjdx.xoj.judge.codesandbox;

import com.jjdx.xoj.judge.codesandbox.impl.ExampleCodeSandbox;
import com.jjdx.xoj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.jjdx.xoj.judge.codesandbox.impl.ThirdPartyCodeSandbox;

/**
 代码沙箱工厂
 */
public class CodeSandboxFactory {
    /**
     根据 沙箱类别type 返回 沙箱实例
     @return 无匹配项时默认返回ExampleCodeSandbox
     */
    public static CodeSandbox newInstance(String type) {
        if ("example".equals(type)) {
            return new ExampleCodeSandbox();
        } else if ("remote".equals(type)) {
            return new RemoteCodeSandbox();
        } else if ("thirdParty".equals(type)) {
            return new ThirdPartyCodeSandbox();
        } else {
            return new ExampleCodeSandbox();
        }
    }
}

package com.xiaosu.rag;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 规则化拒答预检：命中隐私/未公开经营数据关键词时短路拒绝，不调模型。
 * 与 system prompt 中的拒答规则构成双保险（验收 7.4）。
 */
@Component
public class RefusalGuard {

    private static final String REFUSAL_TEXT =
            "抱歉，这个问题涉及的信息不在我能够查询的范围内（涉及个人隐私或未公开的公司数据），无法回答。";

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(CEO|总裁|董事长|总经理|老板).{0,12}(住址|家庭住址|家庭地址|私人地址|房产)"),
            Pattern.compile("(家庭住址|家庭地址|私人住址|家住哪里)"),
            Pattern.compile("20(3\\d|4\\d|5\\d).{0,8}(销售目标|营收目标|经营目标|战略目标)"),
            Pattern.compile("(身份证号|银行卡号|工资条|薪资明细|公积金账号|社保账号)"),
            Pattern.compile("(竞品|竞争对手).{0,10}(报价|数据|方案|策略)")
    );

    public Optional<String> check(String question) {
        for (Pattern p : PATTERNS) {
            if (p.matcher(question).find()) {
                return Optional.of(REFUSAL_TEXT);
            }
        }
        return Optional.empty();
    }
}

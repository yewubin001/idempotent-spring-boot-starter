package com.magfin.idempotent.utils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.Objects;

/**
 * SPEL的工具类
 *
 * @author yewub
 */
public class ExpressionUtil {
    /**
     * 以map对象为上下文变量解析SPEL表达式
     *
     * @param map     表达式执行的上下文变量map
     * @param express SPEL表达式
     * @return 表达式执行结果
     */
    public static String parseExpression(Map<String, Object> map, String express) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(express);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariables(map);
        Object value = expression.getValue(ctx);
        if (Objects.isNull(value)) {
            return null;
        } else {
            return value.toString();
        }
    }
}

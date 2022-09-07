package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.context.env.Environment;
import java.util.Set;

public class DevOrTestCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        ApplicationContext applicationContext = context.getBeanContext().getBean(ApplicationContext.class);
        Set<String> environments = applicationContext.getEnvironment().getActiveNames();
        return environments.contains(Environment.TEST) || environments.contains(Environment.DEVELOPMENT);
    }
}

package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;

import java.util.Set;

public class DevDataCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        ApplicationContext applicationContext = context.getBeanContext().getBean(ApplicationContext.class);
        Set<String> environments = applicationContext.getEnvironment().getActiveNames();
        return environments.contains(RunApplication.ENVIRONMENT_DEV_DATA);
    }
}

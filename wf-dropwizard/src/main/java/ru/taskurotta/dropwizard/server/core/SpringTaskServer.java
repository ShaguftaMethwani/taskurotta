package ru.taskurotta.dropwizard.server.core;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import ru.taskurotta.dropwizard.server.YamlConfigBackend;

import javax.ws.rs.Path;
import java.util.Map;
import java.util.Properties;

public class SpringTaskServer extends Service<TaskServerConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SpringTaskServer.class);

    @Override
    public void initialize(Bootstrap<TaskServerConfig> bootstrap) {
        bootstrap.setName("task-queue-service");
        bootstrap.addBundle(new AssetsBundle("/assets", "/console"));
    }

    @Override
    public void run(final TaskServerConfig configuration, Environment environment)
            throws Exception {

        logger.debug("YAML config custom properties getted[{}]", configuration.getProperties());

        String contextLocation = configuration.getContextLocation();
        AbstractApplicationContext appContext = new ClassPathXmlApplicationContext(new String[]{contextLocation}, false);
        if(configuration.getProperties()!=null && !configuration.getProperties().isEmpty()) {
            appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", configuration.getProperties()));
            if(configuration.getInternalPoolConfig()!=null) {
                Properties internalPoolProperties = configuration.getInternalPoolConfig().asProperties();
                logger.debug("YAML config internal pool properties getted[{}]", internalPoolProperties);
                appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("internalPoolConfigProperties", internalPoolProperties));
            }

        }

        //Initializes YamlConfigBackend bean with actor preferences parsed from DW server YAML configuration
        if(configuration.getActorConfig() != null) {
            appContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                    beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
                        @Override
                        public Object postProcessBeforeInitialization(Object bean, String beanName)
                                throws BeansException {
                            if(bean instanceof YamlConfigBackend) {
                                YamlConfigBackend yamlBean = (YamlConfigBackend)bean;
                                yamlBean.setActorPreferences(configuration.getActorConfig().getAllActorPreferences());
                                yamlBean.setExpirationPolicies(configuration.getActorConfig().getAllExpirationPolicies());
                            }
                            return bean;
                        }
                        @Override
                        public Object postProcessAfterInitialization(Object bean, String beanName)
                                throws BeansException {
                            return bean;
                        }
                    });
                }
            });
        }
        appContext.refresh();

        //Register resources
        Map<String, Object> resources = appContext.getBeansWithAnnotation(Path.class);
        if(resources!=null && !resources.isEmpty()) {
            for(String resourceBeanName: resources.keySet()) {
                Object resourceSingleton = appContext.getBean(resourceBeanName);
                environment.addResource(resourceSingleton);
            }
            logger.info("Registered[{}] resources from application context location [{}]", resources.size(), contextLocation);
        } else {
            //No resources - no fun
            logger.error("Application context [{}] contains no beans annotated with [{}]", contextLocation, Path.class.getName());
            throw new IllegalStateException("No resources found in context["+contextLocation+"]");
        }

        //Register healthchecks
        Map<String, HealthCheck> healthChecks = appContext.getBeansOfType(HealthCheck.class);
        if(healthChecks!=null && !healthChecks.isEmpty()) {
            for(String hcBeanName: healthChecks.keySet()) {
                HealthCheck healthCheck = appContext.getBean(hcBeanName, HealthCheck.class);
                environment.addHealthCheck(healthCheck);
            }
            logger.info("Registered[{}] healthChecks from application context location [{}]", healthChecks.size(), contextLocation);
        }

    }

    public static void main(String[] args) throws Exception {
        new SpringTaskServer().run(args);
    }

}
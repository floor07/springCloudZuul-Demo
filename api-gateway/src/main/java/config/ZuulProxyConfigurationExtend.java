package config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.zuul.ZuulProxyAutoConfiguration;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ZuulProxyConfigurationExtend extends ZuulProxyAutoConfiguration {
    @Autowired
    ZuulConstantReload zuulConstantReload;
    @Autowired
    private ErrorController errorController;
    @Autowired
    private DiscoveryClient discovery;
    @Autowired
    private ServiceRouteMapper serviceRouteMapper;
    @Autowired(required = false)
    private Registration registration;

    @RefreshScope
    @ConfigurationProperties("zuul")
    @Primary
    @Bean
    public ZuulProperties zuulProperties() {
        return new ZuulProperties();
    }


    @Bean
    public ZuulHandlerMapping zuulHandlerMapping(RouteLocator routes) {
        ZuulHandlerMapping mapping = new ZuulHandlerMappingExtend(routes, zuulController());
        mapping.setErrorController(this.errorController);
        return mapping;
    }



    @Bean
    public SimpleRouteLocator simpleRouteLocator() {
        SimpleRouteLocatorExtend simpleRouteLocator= new SimpleRouteLocatorExtend(this.server.getServletPrefix(), this.zuulProperties);
        return simpleRouteLocator;
    }

    @Bean
    public DiscoveryClientRouteLocator discoveryRouteLocator() {
        DiscoveryClientRouteLocatorExtend discoveryClientRouteLocatorExtend= new DiscoveryClientRouteLocatorExtend(this.server.getServletPrefix(),
                this.discovery, this.zuulProperties, this.serviceRouteMapper, this.registration);
        return discoveryClientRouteLocatorExtend;
    }

}

package config;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.ManagedBean;
import java.util.Set;

@RefreshScope
@ConfigurationProperties("zuul")
@Component
public class ZuulConstantReload {
    @Value("${zuul.byPass.url:''}")
    private String byPassUrl;

    
    public Set<String> getByPassUrl() {
        Set set = Sets.newHashSet();
        if (!ObjectUtils.isEmpty(byPassUrl)) {
            String[] arr = byPassUrl.split(",");
            for (String url : arr) {
                set.add(url);
            }
        }
        return set;
    }


    public void setByPassUrl(String byPassUrl) {
        this.byPassUrl = byPassUrl;
    }
}

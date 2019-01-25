package config;

import com.google.common.collect.Sets;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulController;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ZuulHandlerMappingExtend extends ZuulHandlerMapping {
    @Autowired
    ZuulConstantReload zuulConstantReload;

    private final RouteLocator routeLocator;

    private final ZuulController zuul;

    private ErrorController errorController;

    private PathMatcher pathMatcher = new AntPathMatcher();

    private volatile boolean dirty = true;


    public ZuulHandlerMappingExtend(RouteLocator routeLocator, ZuulController zuul) {
        super(routeLocator, zuul);
        this.routeLocator = routeLocator;
        this.zuul = zuul;
        setOrder(-200);
    }

    @Override
    protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
        if (this.errorController != null && urlPath.equals(this.errorController.getErrorPath())) {
            return null;
        }
        if (!isByPathUrl(urlPath, zuulConstantReload.getByPassUrl()) && isIgnoredPath(urlPath, routeLocator.getIgnoredPaths())) {
            return null;
        }
        RequestContext ctx = RequestContext.getCurrentContext();
        if (ctx.containsKey("forward.to")) {
            return null;
        }
        if (this.dirty) {
            synchronized (this) {
                if (this.dirty) {
                    registerHandlers();
                    this.dirty = false;
                }
            }
        }
        return this.realLookupHandler(urlPath, request);
    }


    /**
     * Look up a handler instance for the given URL path.
     * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
     * and various Ant-style pattern matches, e.g. a registered "/t*" matches
     * both "/test" and "/team". For details, see the AntPathMatcher class.
     * <p>Looks for the most exact pattern, where most exact is defined as
     * the longest path pattern.
     *
     * @param urlPath URL the bean is mapped to
     * @param request current HTTP request (to expose the path within the mapping to)
     * @return the associated handler instance, or {@code null} if not found
     * @see #exposePathWithinMapping
     * @see org.springframework.util.AntPathMatcher
     */
    protected Object realLookupHandler(String urlPath, HttpServletRequest request) throws Exception {
        // Direct match?
        Object handler = this.getHandlerMap().get(urlPath);
        if (handler != null) {
            // Bean name or resolved handler?
            if (handler instanceof String) {
                String handlerName = (String) handler;
                handler = getApplicationContext().getBean(handlerName);
            }
            validateHandler(handler, request);
            return buildPathExposingHandler(handler, urlPath, urlPath, null);
        }

        // Pattern match?
        List<String> matchingPatterns = new ArrayList<String>();
        for (String registeredPattern : this.getHandlerMap().keySet()) {
            if (getPathMatcher().match(registeredPattern, urlPath)) {
                matchingPatterns.add(registeredPattern);
            } else if (useTrailingSlashMatch()) {
                if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", urlPath)) {
                    matchingPatterns.add(registeredPattern + "/");
                }
            }
        }

        String bestMatch = null;
        Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
        if (!matchingPatterns.isEmpty()) {
            Collections.sort(matchingPatterns, patternComparator);
            if (logger.isDebugEnabled()) {
                logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
            }
            bestMatch = matchingPatterns.get(0);
        }
        if (bestMatch != null) {
            handler = this.getHandlerMap().get(bestMatch);
            if (handler == null) {
                if (bestMatch.endsWith("/")) {
                    handler = this.getHandlerMap().get(bestMatch.substring(0, bestMatch.length() - 1));
                }
                if (handler == null) {
                    throw new IllegalStateException(
                            "Could not find handler for best pattern match [" + bestMatch + "]");
                }
            }
            // Bean name or resolved handler?
            if (handler instanceof String) {
                String handlerName = (String) handler;
                handler = getApplicationContext().getBean(handlerName);
            }
            validateHandler(handler, request);
            String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, urlPath);

            // There might be multiple 'best patterns', let's make sure we have the correct URI template variables
            // for all of them
            Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
            for (String matchingPattern : matchingPatterns) {
                if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
                    Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
                    Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
                    uriTemplateVariables.putAll(decodedVars);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
            }
            return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
        }

        // No handler found...
        return null;
    }


    private boolean isIgnoredPath(String urlPath, Collection<String> ignored) {
        if (ignored != null) {
            for (String ignoredPath : ignored) {
                if (this.pathMatcher.match(ignoredPath, urlPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void registerHandlers() {
        Collection<Route> routes = this.routeLocator.getRoutes();
        if (routes.isEmpty()) {
            this.logger.warn("No routes found from RouteLocator");
        } else {
            for (Route route : routes) {
                registerHandler(route.getFullPath(), this.zuul);
            }
        }
    }

    private boolean isByPathUrl(String urlPath, Collection<String> pathWhites) {
        if (!ObjectUtils.isEmpty(pathWhites)) {
            for (String whitePath : pathWhites) {
                if (this.pathMatcher.match(whitePath, urlPath)) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request, HandlerExecutionChain chain, CorsConfiguration config) {
        return super.getCorsHandlerExecutionChain(request, chain, config);
    }

    @Override
    public void setErrorController(ErrorController errorController) {
        super.setErrorController(errorController);
    }

    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
    }
}

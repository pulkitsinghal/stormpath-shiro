package com.stormpath.shiro.servlet.env

import com.stormpath.sdk.client.Client
import com.stormpath.sdk.servlet.config.Config
import com.stormpath.sdk.servlet.config.ConfigLoader
import com.stormpath.sdk.servlet.config.impl.DefaultConfigFactory
import com.stormpath.shiro.config.ClientFactory
import com.stormpath.shiro.realm.ApplicationRealm
import com.stormpath.shiro.servlet.ShiroTestSupportWithSystemProperties
import org.apache.shiro.config.Ini
import org.apache.shiro.util.Factory
import org.apache.shiro.web.config.IniFilterChainResolverFactory
import org.apache.shiro.web.filter.mgt.FilterChainResolver
import org.apache.shiro.web.mgt.DefaultWebSecurityManager
import org.easymock.Capture
import org.easymock.IAnswer
import org.easymock.IExpectationSetters
import org.hamcrest.Matchers
import org.testng.annotations.Test

import javax.servlet.ServletContext

import static org.easymock.EasyMock.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertSame

/**
 * Tests for {@link StormpathShiroIniEnvironment}.
 *
 * TODO: this class loads an ApplicationRealm, when that class is loaded it will make a remote connection to api.stormpath.com
 * This makes this class an integration test. A different class could by setting <code>stormpathRealm = com.stub.class.Here</code> in the ini config.
 * The downside os that 'testDefaultCreate' would not longer be testing the default create.  PowerMock might be a solution here.
 *
 */
@Test(singleThreaded = true)
class StormpathShiroIniEnvironmentTest extends ShiroTestSupportWithSystemProperties {

    @Test
    public void testDefaultCreate() {

        def servletContext = mock(ServletContext)

        def clientCapture = new Capture<Client>();

        final def delayedInitMap = new HashMap<String, Object>()
        final def configKey = "config"

        expectConfigFromServletContext(servletContext, delayedInitMap, configKey).anyTimes()

        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES_SOURCES)).andReturn(null)
        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES)).andReturn(null)
        expect(servletContext.getResourceAsStream(anyObject())).andReturn(null).anyTimes()
        servletContext.setAttribute(eq(Client.getName()), capture(clientCapture))

        replay servletContext

        def config = new DefaultConfigFactory().createConfig(servletContext)
        delayedInitMap.put(configKey, config)


        def ini = new Ini()
        doTestWithIni(ini, servletContext)
    }

    @Test
    public void testDefaultCreateWithNoIni() {

        def servletContext = mock(ServletContext)

        def clientCapture = new Capture<Client>();

        final def delayedInitMap = new HashMap<String, Object>()
        final def configKey = "config"

        expectConfigFromServletContext(servletContext, delayedInitMap, configKey).anyTimes()

        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES_SOURCES)).andReturn(null)
        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES)).andReturn(null)
        expect(servletContext.getResourceAsStream(anyObject())).andReturn(null).anyTimes()
        servletContext.setAttribute(eq(Client.getName()), capture(clientCapture))

        replay servletContext

        def config = new DefaultConfigFactory().createConfig(servletContext)
        delayedInitMap.put(configKey, config)


        doTestWithIni(null, servletContext)
    }

    @Test
    public void testCreateWithIniAppHref() {

        def servletContext = mock(ServletContext)

        def clientCapture = new Capture<Client>();

        final def delayedInitMap = new HashMap<String, Object>()
        final def configKey = "config"

        expectConfigFromServletContext(servletContext, delayedInitMap, configKey).anyTimes()

        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES_SOURCES)).andReturn(null)
        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES)).andReturn(null)
        expect(servletContext.getResourceAsStream(anyObject())).andReturn(null).anyTimes()
        servletContext.setAttribute(eq(Client.getName()), capture(clientCapture))

        replay servletContext

        def config = new DefaultConfigFactory().createConfig(servletContext)
        delayedInitMap.put(configKey, config)

        def ini = new Ini()

        doTestWithIni(ini, servletContext)
    }


    @Test
    public void testSimpleFilterConfig() {

        def servletContext = mock(ServletContext)

        def clientCapture = new Capture<Client>();

        final def delayedInitMap = new HashMap<String, Object>()
        final def configKey = "config"

        expectConfigFromServletContext(servletContext, delayedInitMap, configKey).anyTimes()

        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES_SOURCES)).andReturn(null)
        expect(servletContext.getInitParameter(DefaultConfigFactory.STORMPATH_PROPERTIES)).andReturn(null)
        expect(servletContext.getResourceAsStream(anyObject())).andReturn(null).anyTimes()
        servletContext.setAttribute(eq(Client.getName()), capture(clientCapture))

        replay servletContext

        def config = new DefaultConfigFactory().createConfig(servletContext)
        delayedInitMap.put(configKey, config)

        def ini = new Ini()
        // we need to have at least one path defined for the filterChain to be configured.
        ini.setSectionProperty(IniFilterChainResolverFactory.URLS, "/foobar", "anon")

        def configLoader = createNiceMock(ConfigLoader)
        def filterChainResolverFactory = createNiceMock(Factory)
        def filterChainResolver = createNiceMock(FilterChainResolver)

        expect(filterChainResolverFactory.getInstance()).andReturn(filterChainResolver);

        replay configLoader, filterChainResolverFactory, filterChainResolver

        StormpathShiroIniEnvironment environment = new StormpathShiroIniEnvironment() {
            @Override
            protected Factory<? extends FilterChainResolver> getFilterChainResolverFactory(FilterChainResolver originalFilterChainResolver) {
                return filterChainResolverFactory;
            }

            @Override
            protected ConfigLoader ensureConfigLoader() {
                return configLoader
            }
        };
        environment.setIni(ini)
        environment.setServletContext(servletContext)
        environment.init()

        verify servletContext, filterChainResolverFactory, filterChainResolver

        assertNotNull environment.getFilterChainResolver()
    }

    @Test
    public void testDestroyCleanup() {

        def servletContext = createStrictMock(ServletContext)
        def configLoader = createStrictMock(ConfigLoader)
        servletContext.removeAttribute(Client.getName())
        configLoader.destroyConfig(servletContext)

        replay servletContext, configLoader

        def environment = new StormpathShiroIniEnvironment()
        environment.servletContext = servletContext
        environment.configLoader = configLoader
        environment.destroy()

        verify servletContext, configLoader
    }

    private void doTestWithIni(Ini ini, ServletContext servletContext) {

        def configLoader = createNiceMock(ConfigLoader)

        replay configLoader //, app, appResolver

        StormpathShiroIniEnvironment environment = new StormpathShiroIniEnvironment()
        environment.setIni(ini)
        environment.setServletContext(servletContext)
        environment.configLoader = configLoader
        environment.init()

        DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager) environment.getWebSecurityManager();

        verify servletContext //, app, appResolver

        assertThat securityManager.getRealms(), allOf(Matchers.contains(any(ApplicationRealm)), hasSize(1))
        ApplicationRealm realm = securityManager.getRealms().iterator().next()

        def clientObject = environment.objects.get("stormpathClient")
        assertThat clientObject, instanceOf(ClientFactory)
        def actualClient = ((ClientFactory) clientObject).getInstance()
        assertSame realm.getClient(), actualClient
    }

    private IExpectationSetters<ServletContext> expectConfigFromServletContext(ServletContext servletContext, final Map<String, ?> delayedInitMap, String configKey = "config") {
        return expect(servletContext.getAttribute(Config.getName())).andAnswer(new IAnswer<Object>() {
            @Override
            Object answer() throws Throwable {
                return delayedInitMap.get(configKey)
            }
        })
    }

}

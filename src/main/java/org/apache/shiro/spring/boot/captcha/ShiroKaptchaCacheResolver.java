package org.apache.shiro.spring.boot.captcha;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.biz.authc.exception.IncorrectCaptchaException;
import org.apache.shiro.biz.authc.exception.InvalidCaptchaException;
import org.apache.shiro.biz.authc.token.CaptchaAuthenticationToken;
import org.apache.shiro.biz.web.filter.authc.captcha.CaptchaResolver;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.spring.boot.ShiroBizProperties;
import org.apache.shiro.web.util.WebUtils;

import com.google.code.kaptcha.spring.boot.ext.KaptchaResolver;
import com.google.code.kaptcha.spring.boot.ext.exception.CaptchaIncorrectException;
import com.google.code.kaptcha.spring.boot.ext.exception.CaptchaTimeoutException;
import com.google.code.kaptcha.util.Config;

/**
 * 验证Kaptcha生成的验证码
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 */
public class ShiroKaptchaCacheResolver implements KaptchaResolver, CaptchaResolver {

	/**
	 * Name of the session attribute that holds the Captcha name. Only used
	 * internally by this implementation.
	 */
	public static final String CAPTCHA_SESSION_ATTRIBUTE_NAME = ShiroKaptchaCacheResolver.class.getName() + ".Captcha";
	public static final String CAPTCHA_DATE_SESSION_ATTRIBUTE_NAME = ShiroKaptchaCacheResolver.class.getName() + ".Captcha_DATE";

	/**
     * 验证码在Session中存储值的key
     */
	private String captchaStoreKey = CAPTCHA_SESSION_ATTRIBUTE_NAME;
	/**
     * 验证码创建时间在Session中存储值的key
     */
	private String captchaDateStoreKey = CAPTCHA_SESSION_ATTRIBUTE_NAME;
	/**
     * 验证码有效期；单位（毫秒），默认 60000
     */
	private long captchaTimeout = ShiroBizProperties.DEFAULT_CAPTCHA_TIMEOUT;
	
	private final Cache<String, Object> captchaCache;
	
	public ShiroKaptchaCacheResolver(Cache<String, Object> captchaCache) {
		this.captchaCache = captchaCache;
	}
	
	@Override
	public void init(Config config ) {
		if(StringUtils.isNoneEmpty(captchaStoreKey)) {
			this.captchaStoreKey = config.getSessionKey();
		}
		if(StringUtils.isNoneEmpty(captchaDateStoreKey)) {
			this.captchaDateStoreKey = config.getSessionDate();
		}
	}
	
	@Override
	public void init(String captchaStoreKey, String captchaDateStoreKey, long captchaTimeout) {
		if(StringUtils.isNoneEmpty(captchaStoreKey)) {
			this.captchaStoreKey = captchaStoreKey;
		}
		if(StringUtils.isNoneEmpty(captchaDateStoreKey)) {
			this.captchaDateStoreKey = captchaDateStoreKey;
		}
		if(captchaTimeout > 0) {
			this.captchaTimeout = captchaTimeout;
		}
	}
	
	/**
	 * Shiro 验证码接口扩展实现
	 */
	@Override
	public boolean validCaptcha(ServletRequest request, CaptchaAuthenticationToken token)
			throws AuthenticationException {
		// 验证码无效
		if(StringUtils.isEmpty(token.getCaptcha())) {
			throw new IncorrectCaptchaException();
		}
		try {
			
			return this.validCaptcha(WebUtils.toHttp(request), token.getCaptcha());
		} catch (CaptchaIncorrectException e) {
			throw new IncorrectCaptchaException(e);
		} catch (CaptchaTimeoutException e) {
			throw new InvalidCaptchaException(e);
		}
	}

	/**
	 * Kaptcha 验证码接口扩展实现
	 */
	@Override
	public boolean validCaptcha(HttpServletRequest request, String capText)
			throws CaptchaIncorrectException, CaptchaTimeoutException {
		
		// 验证码无效
		if(StringUtils.isEmpty(capText)) {
			throw new CaptchaIncorrectException();
		}
		
		// 历史验证码无效
		String sessionCapText = (String) getCaptchaCache().get(getCaptchaStoreKey());
		if(StringUtils.isEmpty(sessionCapText)) {
			throw new CaptchaIncorrectException();
		}
		// 检查验证码是否过期
		Date sessionCapDate = (Date) getCaptchaCache().get(getCaptchaDateStoreKey());
		if(new Date().getTime() - sessionCapDate.getTime()  > getCaptchaTimeout()) {
			throw new CaptchaTimeoutException();
		}
		
		return StringUtils.equalsIgnoreCase(sessionCapText, capText);
	}

	/**
	 * Kaptcha 验证码接口扩展实现
	 */
	@Override
	public void setCaptcha(HttpServletRequest request, HttpServletResponse response, String capText, Date capDate) {
		
		// store the text in the cache
		getCaptchaCache().put( getCaptchaStoreKey(), (StringUtils.isNotEmpty(capText) ? capText : null));

		// store the date in the cache so that it can be compared
		// against to make sure someone hasn't taken too long to enter  their kaptcha
		getCaptchaCache().put( getCaptchaDateStoreKey(), (capDate != null ? capDate : new Date()));

	}
	
	public String getCaptchaStoreKey() {
		return captchaStoreKey;
	}
	
	public String getCaptchaDateStoreKey() {
		return captchaDateStoreKey;
	}

	public long getCaptchaTimeout() {
		return captchaTimeout;
	}

	public Cache<String, Object> getCaptchaCache() {
		return captchaCache;
	}
	
}

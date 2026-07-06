package com.trustplatform.common;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "a", "label", "h1", "h2", "h3", "h4", "h5", "h6",
                    "p", "i", "b", "u", "strong", "em", "small", "span",
                    "ul", "ol", "li", "br", "hr", "blockquote", "pre", "code",
                    "img", "div", "table", "thead", "tbody", "tr", "th", "td"
            )
            .allowUrlProtocols("http", "https", "mailto")
            .allowAttributes("href", "target").onElements("a")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .allowAttributes("class", "style").globally()
            .toFactory();

    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return POLICY.sanitize(html);
    }
}

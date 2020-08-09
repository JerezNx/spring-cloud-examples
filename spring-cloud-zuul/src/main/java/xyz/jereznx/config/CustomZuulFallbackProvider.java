package xyz.jereznx.config;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author LQL
 * @since Create in 2020/8/9 17:16
 */
public class CustomZuulFallbackProvider implements FallbackProvider {

    private String applicationName;

    public CustomZuulFallbackProvider(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 当前的fallback容错处理逻辑处理的是哪一个服务。可以使用通配符‘*’代表为全部的服务提供容错处理。
     *
     * @return
     */
    @Override
    public String getRoute() {
        return this.applicationName;
    }

    /**
     * 当服务发生错误的时候，如何容错。
     *
     * @param route
     * @param cause
     * @return
     */
    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        cause.printStackTrace();
        return this.executeFallback(HttpStatus.OK, route + "服务熔断",
                "application", "json", "utf-8");
    }

    /**
     * 具体处理过程。
     *
     * @param status       容错处理后的返回状态，如200正常GET请求结果，201正常POST请求结果，404资源找不到错误等。
     *                     使用spring提供的枚举类型对象实现。HttpStatus
     * @param contentMsg   自定义的响应内容。就是反馈给客户端的数据。
     * @param mediaType    响应类型，是响应的主类型， 如： application、text、media。
     * @param subMediaType 响应类型，是响应的子类型， 如： json、stream、html、plain、jpeg、png等。
     * @param charsetName  响应结果的字符集。这里只传递字符集名称，如： utf-8、gbk、big5等。
     * @return ClientHttpResponse 就是响应的具体内容。
     * 相当于一个HttpServletResponse。
     */
    private final ClientHttpResponse executeFallback(final HttpStatus status,
                                                     String contentMsg, String mediaType, String subMediaType, String charsetName) {
        return new ClientHttpResponse() {

            /**
             * 设置响应的头信息
             */
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders header = new HttpHeaders();
                MediaType mt = new MediaType(mediaType, subMediaType, Charset.forName(charsetName));
                header.setContentType(mt);
                return header;
            }

            /**
             * 设置响应体
             * zuul会将本方法返回的输入流数据读取，并通过HttpServletResponse的输出流输出到客户端。
             */
            @Override
            public InputStream getBody() throws IOException {
                String content = contentMsg;
                return new ByteArrayInputStream(content.getBytes());
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回String
             */
            @Override
            public String getStatusText() throws IOException {
                return this.getStatusCode().getReasonPhrase();
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回HttpStatus
             */
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return status;
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回int
             */
            @Override
            public int getRawStatusCode() throws IOException {
                return this.getStatusCode().value();
            }

            /**
             * 回收资源方法
             * 用于回收当前fallback逻辑开启的资源对象的。
             * 不要关闭getBody方法返回的那个输入流对象。
             */
            @Override
            public void close() {
            }
        };
    }
}

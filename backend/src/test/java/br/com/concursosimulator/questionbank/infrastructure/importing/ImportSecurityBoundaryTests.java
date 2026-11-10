package br.com.concursosimulator.questionbank.infrastructure.importing;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class ImportSecurityBoundaryTests {
    static MockHttpServletRequest jsonRequest(){var request=new MockHttpServletRequest();request.setContentType("application/json");return request;}
    @Test void refusesCompressedAndOversizedBodiesBeforeProcessing()throws Exception {
        var filter=new ImportSecurityConfiguration.Capacity();var calls=new AtomicInteger();
        var compressed=new MockHttpServletRequest("POST","/api/v1/admin/question-imports/preview");compressed.setContentType("application/json");compressed.addHeader("Content-Encoding","gzip");
        var response=new MockHttpServletResponse();filter.doFilter(compressed,response,(r,s)->calls.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(415);
        var large=new MockHttpServletRequest("POST","/api/v1/admin/question-imports/preview");large.setContentType("application/json");large.setContent(new byte[16_777_217]);
        response=new MockHttpServletResponse();filter.doFilter(large,response,(r,s)->calls.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(413);assertThat(calls).hasValue(0);
    }
    @Test void rejectsConcurrentPayloadWithoutQueueAndReleasesCapacityAfterFailure()throws Exception {
        var filter=new ImportSecurityConfiguration.Capacity();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var worker=Executors.newSingleThreadExecutor()){
            var active=worker.submit(()->{
                try{filter.doFilter(jsonRequest(),new MockHttpServletResponse(),(r,s)->{
                    entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("fixture timeout");}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
                });}catch(Exception e){throw new IllegalStateException(e);}
            });
            try{
                assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();var denied=new MockHttpServletResponse();var calls=new AtomicInteger();
                filter.doFilter(jsonRequest(),denied,(r,s)->calls.incrementAndGet());
                assertThat(denied.getStatus()).isEqualTo(429);assertThat(denied.getHeader("Retry-After")).isEqualTo("1");assertThat(calls).hasValue(0);
            }finally{release.countDown();}
            active.get(5,TimeUnit.SECONDS);
        }
        assertThatThrownBy(()->filter.doFilter(jsonRequest(),new MockHttpServletResponse(),(r,s)->{throw new IOException("synthetic failure");})).isInstanceOf(IOException.class);
        var calls=new AtomicInteger();filter.doFilter(jsonRequest(),new MockHttpServletResponse(),(r,s)->calls.incrementAndGet());assertThat(calls).hasValue(1);
    }
}

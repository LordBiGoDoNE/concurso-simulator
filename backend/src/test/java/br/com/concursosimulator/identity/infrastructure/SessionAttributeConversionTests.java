package br.com.concursosimulator.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.concursosimulator.identity.application.UserPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.serializer.support.SerializationFailedException;

class SessionAttributeConversionTests {
    final org.springframework.core.convert.ConversionService service =
            SessionAttributeConversion.conversionService(getClass().getClassLoader());

    Object read(byte[] bytes) {
        return service.convert(bytes, TypeDescriptor.valueOf(byte[].class), TypeDescriptor.valueOf(Object.class));
    }

    static byte[] serialize(Object value) throws Exception {
        var out = new ByteArrayOutputStream();
        try (var stream = new ObjectOutputStream(out)) {
            stream.writeObject(value);
        }
        return out.toByteArray();
    }

    /** Troca o nome da classe no descritor serializado (writeUTF: tamanho em 2 bytes + nome). */
    static byte[] rename(byte[] stream, String from, String to) {
        var source = utf(from);
        var target = utf(to);
        var out = new ByteArrayOutputStream();
        for (int i = 0; i < stream.length; ) {
            if (i + source.length <= stream.length
                    && java.util.Arrays.equals(stream, i, i + source.length, source, 0, source.length)) {
                out.writeBytes(target);
                i += source.length;
            } else out.write(stream[i++]);
        }
        return out.toByteArray();
    }

    static byte[] utf(String name) {
        var bytes = name.getBytes(StandardCharsets.UTF_8);
        var result = new byte[bytes.length + 2];
        result[0] = (byte) (bytes.length >> 8);
        result[1] = (byte) bytes.length;
        System.arraycopy(bytes, 0, result, 2, bytes.length);
        return result;
    }

    @Test
    void currentClassesRoundTrip() throws Exception {
        var principal = new UserPrincipal(UUID.randomUUID());
        var bytes = (byte[])
                service.convert(principal, TypeDescriptor.valueOf(Object.class), TypeDescriptor.valueOf(byte[].class));
        assertThat(read(bytes)).isEqualTo(principal);
    }

    @Test
    void removedClassIsAbsent() throws Exception {
        var current = serialize(new UserPrincipal(UUID.randomUUID()));
        var legacy =
                rename(current, UserPrincipal.class.getName(), "br.com.concursosimulator.identity.web.UserPrincipal");
        assertThat(read(legacy)).isNull();
    }

    @Test
    void corruptedBytesStillFail() {
        assertThatThrownBy(() -> read("not a java stream".getBytes(StandardCharsets.UTF_8)))
                .hasRootCauseInstanceOf(java.io.StreamCorruptedException.class)
                .isInstanceOfAny(
                        SerializationFailedException.class,
                        org.springframework.core.convert.ConversionFailedException.class);
    }
}

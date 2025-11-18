package world.usan.usan.common.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

@Converter(autoApply = true)
public class UuidToBytesConverter implements AttributeConverter<UUID, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn(UUID u) {
        if (u == null) {
            return null;
        }
        var bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public UUID convertToEntityAttribute(byte[] b) {
        if (b == null) {
            return null;
        }
        var bb = ByteBuffer.wrap(b);
        return new UUID(bb.getLong(), bb.getLong());
    }
}

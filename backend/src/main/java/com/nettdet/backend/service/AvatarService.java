package com.nettdet.backend.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AvatarService {

    private static final int AVATAR_SIZE_PIXELS = 512;
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String AVATAR_IMAGE_FOLDER = "avatar-images";
    private static final String AVATAR_ASSET_FOLDER = "avatar-assets";

    private final FileObjectService fileObjectService;
    private final FileObjectRepository fileObjectRepository;

    public AvatarService(FileObjectService fileObjectService, FileObjectRepository fileObjectRepository) {
        this.fileObjectService = fileObjectService;
        this.fileObjectRepository = fileObjectRepository;
    }

    @Transactional
    public FileObject getOrCreateAvatar(String avatar) {
        AvatarParts parts = parseAvatarParts(avatar);
        String avatarBaseName = parts.person() + "_" + parts.hair() + "_" + parts.costume();
        String avatarObjectKey = AVATAR_IMAGE_FOLDER + "/" + avatarBaseName + ".png";

        var existingAvatar = fileObjectRepository.findByObjectKey(avatarObjectKey);
        if (existingAvatar.isPresent()) {
            return existingAvatar.get();
        }

        return createAndStoreAvatar(parts, avatarBaseName, avatarObjectKey);
    }

    private FileObject createAndStoreAvatar(AvatarParts parts, String avatarBaseName, String avatarObjectKey) {
        BufferedImage personImage = readAssetImage("person", parts.person());
        BufferedImage hairImage = readAssetImage("hair", parts.hair());
        BufferedImage costumeImage = readAssetImage("costume", parts.costume());

        BufferedImage avatarImage = createAvatarImage(personImage, hairImage, costumeImage, AVATAR_SIZE_PIXELS);
        byte[] pngBytes = toPngBytes(avatarImage);

        return fileObjectService.upload(
                avatarObjectKey,
                pngBytes,
                CONTENT_TYPE_PNG,
                null,
                null,
                ClassroomAccess.PUBLIC,
                avatarBaseName + ".png");
    }

    private BufferedImage readAssetImage(String category, String name) {
        List<String> candidateKeys = List.of(
                AVATAR_ASSET_FOLDER + "/" + category + "/" + name + ".png",
                "/" + AVATAR_ASSET_FOLDER + "/" + category + "/" + name + ".png",
                AVATAR_ASSET_FOLDER + "/" + category + "/" + name,
                "/" + AVATAR_ASSET_FOLDER + "/" + category + "/" + name);

        for (String candidateKey : candidateKeys) {
            if (fileObjectRepository.findByObjectKey(candidateKey).isPresent()) {
                byte[] bytes = fileObjectService.read(candidateKey).bytes();
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image != null) {
                        return image;
                    }
                } catch (IOException ignored) {
                    // Try next candidate key.
                }
            }
        }

        throw new EntityNotFoundException("Avatar asset not found: " + category + "/" + name);
    }

    private BufferedImage createAvatarImage(BufferedImage person, BufferedImage hair, BufferedImage costume, int size) {
        BufferedImage combined = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = combined.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(person, 0, 0, size, size, null);
        graphics.drawImage(hair, 0, 0, size, size, null);
        graphics.drawImage(costume, 0, 0, size, size, null);
        graphics.dispose();

        return combined;
    }

    private byte[] toPngBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode avatar image", exception);
        }
    }

    private AvatarParts parseAvatarParts(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            throw new IllegalArgumentException("Avatar must not be blank");
        }

        String[] parts = avatar.split("_", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("Avatar must have format '<person>_<hair>_<costume>'");
        }

        return new AvatarParts(parts[0], parts[1], parts[2]);
    }

    private record AvatarParts(String person, String hair, String costume) {
    }
}

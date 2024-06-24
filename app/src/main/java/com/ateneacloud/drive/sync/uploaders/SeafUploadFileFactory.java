package com.ateneacloud.drive.sync.uploaders;

public class SeafUploadFileFactory {

    /**
     * Crea una instancia de SeafUploadFile basada en el tipo de elemento de sincronización.
     *
     * @param seafSyncItem El elemento de sincronización.
     * @return Una instancia de SeafUploadFile.
     */
   /* public static SeafUploadFile createFrom(SeafSyncFileItem seafSyncItem) {
        switch (seafSyncItem.getItemType()) {
            case SeafSyncItemTypeFile:
                return createFromSyncFileType(seafSyncItem);
            case SeafSyncItemTypeAsset:
                return createFromSyncAssetType(seafSyncItem);
            default:
                return null;
        }
    }*/

    /**
     * Crea una instancia de SeafUploadFile desde una URL de archivo.
     *
     * @param fileURL La URL del archivo.
     * @return Una instancia de SeafUploadFile.
     */
//    public static SeafUploadFile createFromURL(File fileURL) {
//        SeafUploadFile uploadFile = new SeafUploadFile(fileURL.getPath());
//        uploadFile.setRetryable(true);
//        uploadFile.setRemoveSourceAfterUpload(false);
//        uploadFile.setAutoSync(false);
//        uploadFile.setOverwrite(true);
//
//        return uploadFile;
//    }

    /**
     * Crea una instancia de SeafUploadFile desde un elemento de sincronización basado en archivo.
     *
     * @param seafSyncItem El elemento de sincronización.
     * @return Una instancia de SeafUploadFile.
     */
    /*public static SeafUploadFile createFromSyncFileType(SeafSyncItemProtocol seafSyncItem) {
        SeafUploadFile uploadFile = new SeafUploadFile(seafSyncItem.getPath().getPath());
        uploadFile.setRetryable(true);
        uploadFile.setRemoveSourceAfterUpload(false);
        uploadFile.setAutoSync(false);
        uploadFile.setOverwrite(true);

        return uploadFile;
    }*/

    /**
     * Crea una instancia de SeafUploadFile desde un elemento de sincronización basado en activo (asset).
     *
     * @param seafSyncItem El elemento de sincronización.
     * @return Una instancia de SeafUploadFile.
     */
//    public static SeafUploadFile createFromSyncAssetType(SeafSyncItemProtocol seafSyncItem) {
//        SeafSyncAssetItem seafSyncAssetItem = (SeafSyncAssetItem) seafSyncItem;
//
//        // Calcula la ruta del activo (asset)
//        SeafPhotoAsset photoAsset = new SeafPhotoAsset(seafSyncAssetItem.getAsset());
//        String uploadsDir = SeafStorage.uniqueDirUnder(SeafStorage.sharedObject.getUploadsDir());
//        String path = uploadsDir + File.separator + photoAsset.getName();
//
//        // Crea el UploadFile
//        SeafUploadFile file = new SeafUploadFile(path);
//        file.setRetryable(false);
//        file.setAutoSync(true);
//        file.setOverwrite(true);
//        file.setPHAsset(seafSyncAssetItem.getAsset(), photoAsset.getALAssetURL());
//
//        return file;
//    }
}

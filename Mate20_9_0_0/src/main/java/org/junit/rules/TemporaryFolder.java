package org.junit.rules;

import java.io.File;
import java.io.IOException;

public class TemporaryFolder extends ExternalResource {
    private File folder;
    private final File parentFolder;

    public TemporaryFolder() {
        this(null);
    }

    public TemporaryFolder(File parentFolder) {
        this.parentFolder = parentFolder;
    }

    protected void before() throws Throwable {
        create();
    }

    protected void after() {
        delete();
    }

    public void create() throws IOException {
        this.folder = createTemporaryFolderIn(this.parentFolder);
    }

    public File newFile(String fileName) throws IOException {
        File file = new File(getRoot(), fileName);
        if (file.createNewFile()) {
            return file;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("a file with the name '");
        stringBuilder.append(fileName);
        stringBuilder.append("' already exists in the test folder");
        throw new IOException(stringBuilder.toString());
    }

    public File newFile() throws IOException {
        return File.createTempFile("junit", null, getRoot());
    }

    public File newFolder(String folder) throws IOException {
        return newFolder(folder);
    }

    public File newFolder(String... folderNames) throws IOException {
        File file = getRoot();
        int i = 0;
        while (i < folderNames.length) {
            String folderName = folderNames[i];
            validateFolderName(folderName);
            file = new File(file, folderName);
            if (file.mkdir() || !isLastElementInArray(i, folderNames)) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("a folder with the name '");
                stringBuilder.append(folderName);
                stringBuilder.append("' already exists");
                throw new IOException(stringBuilder.toString());
            }
        }
        return file;
    }

    private void validateFolderName(String folderName) throws IOException {
        if (new File(folderName).getParent() != null) {
            throw new IOException("Folder name cannot consist of multiple path components separated by a file separator. Please use newFolder('MyParentFolder','MyFolder') to create hierarchies of folders");
        }
    }

    private boolean isLastElementInArray(int index, String[] array) {
        return index == array.length - 1;
    }

    public File newFolder() throws IOException {
        return createTemporaryFolderIn(getRoot());
    }

    private File createTemporaryFolderIn(File parentFolder) throws IOException {
        File createdFolder = File.createTempFile("junit", "", parentFolder);
        createdFolder.delete();
        createdFolder.mkdir();
        return createdFolder;
    }

    public File getRoot() {
        if (this.folder != null) {
            return this.folder;
        }
        throw new IllegalStateException("the temporary folder has not yet been created");
    }

    public void delete() {
        if (this.folder != null) {
            recursiveDelete(this.folder);
        }
    }

    private void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        file.delete();
    }
}

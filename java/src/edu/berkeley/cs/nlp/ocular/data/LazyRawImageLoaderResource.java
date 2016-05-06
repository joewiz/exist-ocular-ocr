package edu.berkeley.cs.nlp.ocular.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.berkeley.cs.nlp.ocular.model.CharacterTemplate;
import edu.berkeley.cs.nlp.ocular.util.FileUtil;

import org.exist.util.io.Resource;


/**
 * A dataset loader that reads files the files recursively, in lexicographical 
 * order.  Images are loaded only as they are needed (lazily), and then stored
 * in memory for later use.
 * 
 * @author Dan Garrette (dhgarrette@gmail.com)
 */
public class LazyRawImageLoaderResource {

	public static List<Document> loadDocuments(String inputPath, String extractedLinesPath, int numDocs, int numDocsToSkip, int lineHeight, int numExtractIterations, int numExtractRestarts, int extractLineHeight) { 
		return loadDocuments(inputPath, extractedLinesPath, numDocs, numDocsToSkip, true, lineHeight, 0.12, false, numExtractIterations, numExtractRestarts, extractLineHeight); 
	}

	public static List<Document> loadDocuments(String inputPath, String extractedLinesPath, int numDocs, int numDocsToSkip, boolean uniformLineHeight, int lineHeight, double binarizeThreshold, boolean crop, int numExtractIterations, int numExtractRestarts, int extractLineHeight) {
		return loadDocuments(Arrays.asList(inputPath), extractedLinesPath, numDocs, numDocsToSkip, uniformLineHeight, lineHeight, binarizeThreshold, crop, numExtractIterations, numExtractRestarts, extractLineHeight);
	}

	public static List<Document> loadDocuments(List<String> inputPaths, String extractedLinesPath, int numDocs, int numDocsToSkip, int lineHeight, int numExtractIterations, int numExtractRestarts, int extractLineHeight) { 
		return loadDocuments(inputPaths, extractedLinesPath, numDocs, numDocsToSkip, true, lineHeight, 0.12, false, numExtractIterations, numExtractRestarts, extractLineHeight); 
	}

	public static List<Document> loadDocuments(List<String> inputPaths, String extractedLinesPath, int numDocs, int numDocsToSkip, boolean uniformLineHeight, int lineHeight, double binarizeThreshold, boolean crop, int numExtractIterations, int numExtractRestarts, int extractLineHeight) {
		List<Document> lazyDocs = new ArrayList<Document>();
		for (String inputPath : inputPaths) {
			lazyDocs.addAll(loadDocumentsFromDir(inputPath, extractedLinesPath, numDocs, numDocsToSkip, uniformLineHeight, lineHeight, binarizeThreshold, crop, numExtractIterations, numExtractRestarts, extractLineHeight));
		}

		int actualNumDocsToSkip = Math.min(lazyDocs.size(), numDocsToSkip);
		int actualNumDocsToUse = Math.min(lazyDocs.size() - actualNumDocsToSkip, numDocs <= 0 ? Integer.MAX_VALUE : numDocs);
		System.out.println("Using "+actualNumDocsToUse+" documents (skipping "+actualNumDocsToSkip+")");
		for (int docNum = 0; docNum < actualNumDocsToSkip; ++docNum) {
			Document lazyDoc = lazyDocs.get(docNum);
			System.out.println("  Skipping the first "+numDocsToSkip+" documents: " + lazyDoc.baseName());
		}

		List<Document> documents = new ArrayList<Document>();
		for (int docNum = actualNumDocsToSkip; docNum < actualNumDocsToSkip + actualNumDocsToUse; ++docNum) {
			Document lazyDoc = lazyDocs.get(docNum);
			System.out.println("  Using " + lazyDoc.baseName());
			documents.add(lazyDoc);
		}
		return documents;
	}
	
	private static List<Document> loadDocumentsFromDir(String inputPath, String extractedLinesPath, int numDocs, int numDocsToSkip, boolean uniformLineHeight, int lineHeight, double binarizeThreshold, boolean crop, int numExtractIterations, int numExtractRestarts, int extractLineHeight) {
		lineHeight = uniformLineHeight ? lineHeight : -1;

		File dir = new Resource(inputPath);
		System.out.println("Reading data from [" + dir + "], which " + (dir.exists() ? "exists" : "does not exist"));
		List<File> dirList = FileUtil.recursiveFiles(dir);

		List<Document> lazyDocs = new ArrayList<Document>();
		for (File f : dirList) {
			if (f.getName().endsWith(".txt"))
				continue;
			else if (f.getName().endsWith(".pdf")) {
				int numPages = PdfImageReaderResource.numPagesInPdf(f);
				for (int pageNumber = 1; pageNumber <= numPages; ++pageNumber) {
					lazyDocs.add(new LazyRawPdfImageDocumentResource(f, pageNumber, inputPath, lineHeight, binarizeThreshold, crop, extractedLinesPath, numExtractIterations, numExtractRestarts, extractLineHeight));
				}
			}
			else {
				lazyDocs.add(new LazyRawSingleImageDocumentResource(f, inputPath, lineHeight, binarizeThreshold, crop, extractedLinesPath, numExtractIterations, numExtractRestarts, extractLineHeight));
			}
		}

		Collections.sort(lazyDocs, new Comparator<Document>() {
			public int compare(Document o1, Document o2) {
				return o1.baseName().compareTo(o2.baseName());
			}
		});
		
		return lazyDocs;
	}
}

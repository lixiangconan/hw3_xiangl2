package edu.cmu.xiangl2.annotators;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Answer;

/**
 * Analysis engine which annotates questions and answers in the input file.
 */
public class TextElementAnnotator extends JCasAnnotator_ImplBase {

  /**
   * The major process for generating Question and Answer annotations.
   * <p>
   * This method read input file line by line, then check the first character in each line. If the
   * first character is 'Q', then this line is annotates as a question. Otherwise, if the first
   * character is 'A', the this line is annotates as an answer.
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    String docText = aJCas.getDocumentText();
    docText.replace("\r\n", "\n");
    int start = 0;
    for (int i = 0; i < docText.length(); i++) {
      if (docText.charAt(i) == '\n') {
        int end = i;
        if (docText.charAt(start) == 'Q') {
          Question annotation = new Question(aJCas);
          annotation.setBegin(start);
          annotation.setEnd(end);
          annotation.setConfidence(1);
          annotation.setCasProcessorId(this.getClass().getName());
          annotation.addToIndexes();
        } else if (docText.charAt(start) == 'A') {
          Answer annotation = new Answer(aJCas);
          annotation.setBegin(start);
          annotation.setEnd(end);
          annotation.setConfidence(1);
          annotation.setCasProcessorId(this.getClass().getName());
          if (docText.charAt(start + 2) == '1') {
            annotation.setIsCorrect(true);
          } else {
            annotation.setIsCorrect(false);
          }
          annotation.addToIndexes();
        }
        start = i + 1;
      }
    }
  }

}

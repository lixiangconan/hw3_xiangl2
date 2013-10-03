package edu.cmu.xiangl2.annotators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import edu.cmu.deiis.types.*;

import org.cleartk.ne.type.*;

/**
 * Analysis engine which assign a score to each answer to indicate how likely it could be a correct
 * answer.
 * <p>
 * The scoring method is based on N-Gram and Name Entity hit rate between sentence and answer. A
 * high score shows that the answer is likely to be a correct answer.
 */
public class NGramAnswerScoring extends JCasAnnotator_ImplBase {

  /**
   * The major scoring process.
   * <p>
   * This method assign a score to each answer to show how likely it could be a correct answer. The
   * score is calculated using N-Gram and Name Entity hit rate. That is to say, the system counts
   * the number of N-Gram and Name Entity annotations in the answer sentences that could be matched
   * with N-Gram and Name Entity annotations in the question sentence, then calculates the ratio of
   * matched annotation number to total annotation number.
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    FSIndex<?> questionIndex = aJCas.getAnnotationIndex(Question.type);
    Iterator<?> questionIter = questionIndex.iterator();
    List<NGram> questionNgram = null;
    Question question = (Question) questionIter.next();
    int startp = question.getBegin();
    int endp = question.getEnd();
    questionNgram = getSentenceNgram(aJCas, (Annotation) question);

    FSIndex<?> answerIndex = aJCas.getAnnotationIndex(Answer.type);
    Iterator<?> answerIter = answerIndex.iterator();
    List<NGram> answerNgram = null;
    while (answerIter.hasNext()) {
      Answer answer = (Answer) answerIter.next();
      answerNgram = getSentenceNgram(aJCas, (Annotation) answer);
      int total = answerNgram.size();
      int match = 0;
      for (int j = 0; j < answerNgram.size(); j++) {
        for (int i = 0; i < questionNgram.size(); i++) {
          if (aJCas
                  .getDocumentText()
                  .substring(questionNgram.get(i).getBegin(), questionNgram.get(i).getEnd())
                  .equals(aJCas.getDocumentText().substring(answerNgram.get(j).getBegin(),
                          answerNgram.get(j).getEnd()))) {
            match += 1;
            break;
          }
        }
      }

      double score;
      FSIndex<?> namedEntityIndex = aJCas.getAnnotationIndex(NamedEntityMention.type);
      if (namedEntityIndex.size() == 0) {
        score = match / (double) total;
      } else {
        List<NamedEntityMention> answerNamedEntity = getAnswerNamedEntity(aJCas, answer);
        int totalNamedEntity = answerNamedEntity.size();
        int matchNamedEntity = 0;
        for (int i = 0; i < answerNamedEntity.size(); i++) {
          FSArray mention = answerNamedEntity.get(i).getMentionedEntity().getMentions();
          for (int j = 0; j < mention.size(); j++) {
            if (((NamedEntityMention) mention.get(j)).getBegin() >= startp
                    && ((NamedEntityMention) mention.get(j)).getEnd() <= endp) {
              matchNamedEntity += 1;
              break;
            }
          }
        }
        score = (match + matchNamedEntity*2) / (double) (total + totalNamedEntity*2);
      }

      AnswerScore annotation = new AnswerScore(aJCas);
      annotation.setBegin(answer.getBegin());
      annotation.setEnd(answer.getEnd());
      annotation.setConfidence(1);
      annotation.setCasProcessorId(this.getClass().getName());
      annotation.setAnswer(answer);
      annotation.setScore(score);
      annotation.addToIndexes();
    }
  }

  private List<NGram> getSentenceNgram(JCas aJCas, Annotation annotation) {
    List<NGram> result = new ArrayList<NGram>();
    FSIndex<?> ngramIndex = aJCas.getAnnotationIndex(NGram.type);
    Iterator<?> ngramIter = ngramIndex.iterator();
    int begin, end;
    begin = annotation.getBegin();
    end = annotation.getEnd();
    while (ngramIter.hasNext()) {
      NGram ngram = (NGram) ngramIter.next();
      if (ngram.getBegin() >= begin && ngram.getEnd() <= end) {
        result.add(ngram);
      }
    }
    return result;
  }

  private List<NamedEntityMention> getAnswerNamedEntity(JCas aJCas, Answer annotation) {
    List<NamedEntityMention> result = new ArrayList<NamedEntityMention>();
    FSIndex<?> namedEntityIndex = aJCas.getAnnotationIndex(NamedEntityMention.type);
    Iterator<?> namedEntityIter = namedEntityIndex.iterator();
    int begin, end;
    begin = annotation.getBegin();
    end = annotation.getEnd();
    while (namedEntityIter.hasNext()) {
      NamedEntityMention namedEntity = (NamedEntityMention) namedEntityIter.next();
      if (namedEntity.getBegin() >= begin && namedEntity.getEnd() <= end) {
        result.add(namedEntity);
      }
    }
    return result;
  }
}

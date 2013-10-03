package edu.cmu.xiangl2.CASconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;

public class MyConsumer extends CasConsumer_ImplBase {
  
  private ArrayList<Double> allPrecision = new ArrayList<Double>();
  
  /**
   * The method which calculate the average precision for all input files.
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
  IOException {
    double ave = 0;
    for(int i=0; i<allPrecision.size(); i++){
      ave = ave + allPrecision.get(i);
    }
    ave = ave/allPrecision.size();
    
    System.out.println();
    System.out.println("Average Precision: "+ave);
  }
  
  /**
   * The method which choose answers and calculate precision for each question.
   * <p>
   * According to the result of the pipeline, this method firstly order the answers according to their score. Then, N sentences with the
   * highest scores are chosen as the output answers. Finally, it counts how many chosen answers are
   * actually correct, and calculates a precision to measure the system performance.
   */
  @Override
  public void processCas(CAS aCAS) throws ResourceProcessException {
    JCas aJCas;
    try {
      aJCas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    
    int N = getCorrectNumber(aJCas);
    System.out.println();
    List<AnswerScore> correctAnswer = null;
    correctAnswer = chooseCorrectAnswer(aJCas, N);
    
    System.out.println("Qustion:");
    FSIndex<?> questionIndex = aJCas.getAnnotationIndex(Question.type);
    Iterator<?> questionIter = questionIndex.iterator();
    while (questionIter.hasNext()) {
      Question question = (Question) questionIter.next();
      System.out.println(aJCas.getDocumentText().substring(question.getBegin()+2,question.getEnd()-1));
    }
    
    System.out.println("Answers:");
    for (int i = 0; i < correctAnswer.size(); i++) {
      System.out.println(aJCas.getDocumentText().substring(correctAnswer.get(i).getBegin()+2,
              correctAnswer.get(i).getEnd()-1));
    }
    int correctAnswerNumber = 0;
    double precision;
    for (int i = 0; i < correctAnswer.size(); i++) {
      if (correctAnswer.get(i).getAnswer().getIsCorrect())
        correctAnswerNumber = correctAnswerNumber + 1;
    }
    precision = correctAnswerNumber / (double) N;
    System.out.println("Precision at " + N +": " + precision);   
    allPrecision.add(precision);
  }
  
  private int getCorrectNumber(JCas aJCas) {
    FSIndex<?> answerIndex = aJCas.getAnnotationIndex(Answer.type);
    Iterator<?> answerIter = answerIndex.iterator();
    int N = 0;
    while (answerIter.hasNext()) {
      Answer answer = (Answer) answerIter.next();
      if (answer.getIsCorrect())
        N = N + 1;
    }
    return N;
  }
  
  private class ComparatorAnswerScore implements Comparator {

    public int compare(Object arg0, Object arg1) {
      AnswerScore answerScore0 = (AnswerScore) arg0;
      AnswerScore answerScore1 = (AnswerScore) arg1;
      double delta = answerScore0.getScore() - answerScore1.getScore();
      if (delta > 0.00001)
        return 1;
      if (delta < -0.00001)
        return -1;
      return 0;
    }
  }

  /*
   * This method find n answers with the highest score.
   */
  private List<AnswerScore> chooseCorrectAnswer(JCas aJCas, int correctNuber) {
    List<AnswerScore> result = new ArrayList<AnswerScore>();
    List<AnswerScore> answerScoreList = new ArrayList<AnswerScore>();

    FSIndex<?> answerScoreIndex = aJCas.getAnnotationIndex(AnswerScore.type);
    Iterator<?> answerScoreIter = answerScoreIndex.iterator();

    while (answerScoreIter.hasNext()) {
      AnswerScore answerScore = (AnswerScore) answerScoreIter.next();
      answerScoreList.add(answerScore);
    }

    ComparatorAnswerScore comparator = new ComparatorAnswerScore();
    Collections.sort(answerScoreList, comparator);
    Collections.reverse(answerScoreList);
    for (int i = 0; i < correctNuber && i < answerScoreList.size(); i++) {
      result.add(answerScoreList.get(i));
    }
    return result;
  }
}

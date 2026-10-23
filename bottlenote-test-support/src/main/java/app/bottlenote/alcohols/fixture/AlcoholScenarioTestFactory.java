package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

public final class AlcoholScenarioTestFactory {
  private AlcoholScenarioTestFactory() {}
  public static ScenarioInputConfig choices(String... values) {
    return new ScenarioInputConfig(true,Arrays.stream(values).map(v -> new ScenarioInputConfig.ScenarioChoice(v,v,TextNode.valueOf(v))).toList(),null,null,null,null);
  }
  public static ScenarioInputConfig range(int min,int max,int step) {
    return new ScenarioInputConfig(true,null,BigDecimal.valueOf(min),BigDecimal.valueOf(max),BigDecimal.valueOf(step),null);
  }
  public static ScenarioQuestionItem choice(long id,long stage,int branch,String title,Source source,Long attribute,String... values) {
    return new ScenarioQuestionItem(id,stage,branch,title,source,attribute,Input.SINGLE_CHOICE,choices(values));
  }
  public static ScenarioQuestionItem number(long id,long stage,int branch,String title,Source source,Long attribute,int min,int max,int step) {
    return new ScenarioQuestionItem(id,stage,branch,title,source,attribute,Input.NUMBER_RANGE,range(min,max,step));
  }
  public static ScenarioTransitionItem rule(long id,long from,int priority,Operator operator,String value,Long to) {
    return new ScenarioTransitionItem(id,from,priority,new ScenarioPredicate(operator,value==null?null:TextNode.valueOf(value)),to==null?Target.END:Target.QUESTION,to);
  }
  public static ScenarioDefinition sevenStages(Long experience,Long price) {
    var stages=IntStream.rangeClosed(1,7).mapToObj(i -> new ScenarioStageItem(-(long)i,i)).toList();
    var questions=List.of(
      choice(-11,-1,1,"q1",Source.ATTRIBUTE,experience,"newbie","couple","tried","expert"),
      choice(-21,-2,1,"q2a",Source.ATTRIBUTE,experience,"newbie","tried"),
      choice(-22,-2,2,"q2b",Source.ATTRIBUTE,experience,"newbie","tried"),
      number(-23,-2,3,"q2c",Source.ATTRIBUTE,price,0,100000,1000),
      choice(-31,-3,1,"q3",Source.CATEGORY,null,"SINGLE_MALT","BOURBON","OTHER"),
      choice(-41,-4,1,"q4a",Source.TASTING_GROUP,null,"PEAT","SWEET"),
      number(-42,-4,2,"q4b",Source.ABV,null,0,100,1),
      choice(-43,-4,3,"q4c",Source.TASTING_GROUP,null,"PEAT","SWEET"),
      number(-51,-5,1,"q5",Source.ATTRIBUTE,price,0,100000,1000),
      number(-61,-6,1,"q6",Source.ABV,null,0,100,1),
      choice(-71,-7,1,"q7",Source.ATTRIBUTE,experience,"newbie","tried"));
    var rules=List.of(
      rule(-101,-11,1,Operator.EQ,"expert",-31L),
      rule(-102,-11,2,Operator.EQ,"couple",-22L),
      rule(-103,-11,3,Operator.EQ,"tried",-23L),
      rule(-104,-11,4,Operator.ALWAYS,null,-21L),
      rule(-105,-21,1,Operator.ALWAYS,null,-31L),
      rule(-106,-22,1,Operator.ALWAYS,null,-31L),
      rule(-107,-23,1,Operator.ALWAYS,null,-31L),
      rule(-108,-31,1,Operator.EQ,"BOURBON",-42L),
      rule(-109,-31,2,Operator.EQ,"OTHER",-43L),
      rule(-110,-31,3,Operator.ALWAYS,null,-41L),
      rule(-111,-41,1,Operator.ALWAYS,null,-51L),
      rule(-112,-42,1,Operator.ALWAYS,null,-61L),
      rule(-113,-43,1,Operator.ALWAYS,null,-51L),
      rule(-114,-51,1,Operator.ALWAYS,null,-61L),
      rule(-115,-61,1,Operator.ALWAYS,null,-71L),
      rule(-116,-71,1,Operator.ALWAYS,null,null));
    return new ScenarioDefinition(1,-11L,stages,questions,rules);
  }
  public static ScenarioDefinition oneQuestion() {
    return new ScenarioDefinition(1,-11L,List.of(new ScenarioStageItem(-1L,1)),
      List.of(choice(-11,-1,1,"q1",Source.CATEGORY,null,"SINGLE_MALT","BOURBON")),
      List.of(rule(-101,-11,1,Operator.ALWAYS,null,null)));
  }
}

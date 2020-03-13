package cn.komikun;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：komikun
 * @date ：Created in 2020-03-11 15,18
 * @description：Mips Assembler
 * @modified By：komikun
 * @version,1.0
 */
public class Assembler {
  static final Logger logger = LoggerFactory.getLogger(Assembler.class);
  public static void main(String[] args) throws IOException {
    String filepath = "src/main/resources/data.asm";
    File file = new File(filepath);
    Program p = new Program(0, new HashMap<String, Integer>());
    String contents = new String(Files.readAllBytes(Paths.get(filepath)));
    p.exeProgram(contents);
  }

}

class Program {
  static final Logger logger = LoggerFactory.getLogger(Program.class);
  private int PC;
  private Map<String, Integer> labelMap;

  public Program(int PC, Map<String, Integer> labelMap) {
    this.PC = PC;
    this.labelMap = labelMap;
  }

  public String exeProgram(String str) {
    StringBuilder sb = new StringBuilder();
    String[] strs = str.split("(\r\n|\r|\n)", -1);
    if (!readLabels(strs)) {
      return "Wrong Label:label must end with ':'";
    }
    for (String s : strs) {
      this.PC += 4;
      logger.info("Start parsing :" + s);
      String[] list = s.split(" ");
      // 0 -> label 1 -> orderName 2 -> para 3 -> comment
      Integer result = 0;
      if (list.length != 1) {
        String orderName = list[1].toLowerCase();
        if (ROrder.isROrder(orderName)) {
          if (list.length < 3) {
            result = ROrder.transformOrder2Binary(orderName, new ArrayList<String>());
          } else {
            result = ROrder
                .transformOrder2Binary(orderName, Util.preprocess(list[2].toLowerCase()));
          }
        } else if (list.length < 3) {
          result = null;
        } else if (IOrder.isIOrder(orderName)) {
          result = IOrder
              .transformOrder2Binary(orderName, Util.preprocess(list[2].toLowerCase()), this.PC,
                  this.labelMap);
        } else if (JOrder.isJOrder(orderName)) {
          result = JOrder.transformOrder2Binary(orderName, Util.preprocess(list[2].toLowerCase()),
              this.labelMap);
        } else {
          result = null;
        }
        if (result != null) {
          sb.append("[0x" + String.format("%08x", result) + "] ");
          sb.append(list[1] + " " + list[2]);
          sb.append("\n");
        } else {
          sb.append("illegal farmat:" + s);
          sb.append("\n");
        }
      }

    }
    logger.info(sb.toString());
    return sb.toString();
  }

  private Boolean readLabels(String[] str) {
    int lineNum = 0;
    for (String s : str) {
      String label = s.split(" ")[0].toLowerCase();
      if (!label.equals("")) {
        if (label.endsWith(":")) {
          this.labelMap.put(label.substring(0, label.length() - 1), lineNum);
        } else {
          return false;
        }
      }
      lineNum += 4;
    }
    return true;
  }
}

class Order {

  static Map<String, Integer> registersMap = ImmutableMap.<String, Integer>builder().put("$zero", 0)
      .put("$at", 1).put("$v0", 2).put("$v1", 3).put("$a0", 4).put("$a1", 5).put("$a2", 6)
      .put("$a3", 7).put("$t0", 8).put("$t1", 9).put("$t2", 10).put("$t3", 11).put("$t4", 12)
      .put("$t5", 13).put("$t6", 14).put("$t7", 15).put("$s0", 16).put("$s1", 17).put("$s2", 18)
      .put("$s3", 19).put("$s4", 20).put("$s5", 21).put("$s6", 22).put("$s7", 23).put("$t8", 24)
      .put("$t9", 25).put("$k0", 26).put("$k1", 27).put("$gp", 28).put("$sp", 29).put("$fp", 30)
      .put("$ra", 31)
      .put("$0", 0).put("$1", 1).put("$2", 2).put("$3", 3).put("$4", 4).put("$5", 5).put("$6", 6)
      .put("$7", 7).put("$8", 8).put("$9", 9).put("$10", 10).put("$11", 11).put("$12", 12)
      .put("$13", 13).put("$14", 14).put("$15", 15).put("$16", 16).put("$17", 17).put("$18", 18)
      .put("$19", 19).put("$20", 20).put("$21", 21).put("$22", 22).put("$23", 23).put("$24", 24)
      .put("$25", 25).put("$26", 26).put("$27", 27).put("$28", 28).put("$29", 29).put("$30", 30)
      .put("$31", 31).build();
}

class ROrder extends Order {
  static final Logger logger = LoggerFactory.getLogger(ROrder.class);

  private static Set<String> orders = ImmutableSet.<String>builder()
      .add("add", "addu", "sub", "subu", "slt", "sltu", "and", "or", "xor", "nor", "sll", "srl",
          "sllv", "srlv", "srav", "mult", "multu", "div", "divu", "jalr", "eret", "syscall", "jr")
      .build();

  public static Boolean isROrder(String orderName) {
    return orders.contains(orderName);
  }

  public static Integer transformOrder2Binary(String orderName, List<String> paraList) {
    String rs, rt, rd;
    switch (paraList.size()) {
      case 0:
        if (orderName.equals("eret")) {
          return (16 << 26) | ((16 & 31) << 21) | (24 & 63);
        } else if (orderName.equals("syscall")) {
          return (0 << 26) | (12 & 63);
        }
        return null;
      case 1:
        rs = paraList.get(0);
        if (orderName.equals("jr")) {
          if (registersMap.containsKey(rs)) {
            return (0 << 26) | (8 & 63) | (registersMap.get(rs) << 21);
          } else {
            return null;
          }
        }
        return null;
      case 2:
        rs = paraList.get(0);
        rt = paraList.get(1);
        if (!registersMap.containsKey(rs) || !registersMap.containsKey(rt)) {
          return null;
        }
        switch (orderName) {
          case "mult":
            return (0 << 26) | (24 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "multu":
            return (0 << 26) | (25 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "div":
            return (0 << 26) | (26 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "divu":
            return (0 << 26) | (27 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "jalr":
            return (0 << 26) | (9 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 11);
        }
        return null;
      case 3:
        rs = paraList.get(1);
        rt = paraList.get(2);
        rd = paraList.get(0);
        if (registersMap.containsKey(rs) && registersMap.containsKey(rd)) {
          if (registersMap.containsKey(rt)) {
            switch (orderName) {
              case "add":
                return (0 << 26) | (32 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "addu":
                return (0 << 26) | (33 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "sub":
                return (0 << 26) | (34 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "subu":
                return (0 << 26) | (35 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "slt":
                return (0 << 26) | (42 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "sltu":
                return (0 << 26) | (43 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "and":
                return (0 << 26) | (36 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "or":
                return (0 << 26) | (37 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "xor":
                return (0 << 26) | (38 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "nor":
                return (0 << 26) | (39 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "sllv":
                return (0 << 26) | (4 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "srlv":
                return (0 << 26) | (6 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
              case "srav":
                return (0 << 26) | (7 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | (registersMap.get(rt) << 16);
            }
          } else if (Util.isNumber(rt)) {
            switch (orderName) {
              case "sll":
                return (0 << 26) | (registersMap.get(rd) << 11) | (registersMap.get(rs) << 21) | (
                    (Util.str2Num(rt) & 31) << 6);
              case "srl":
                return (0 << 26) | (2 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | ((Util.str2Num(rt) & 31) << 6);
              case "sra":
                return (0 << 26) | (3 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | ((Util.str2Num(rt) & 31) << 6);
            }
          } else {
            return null;
          }

        }
    }
    return null;
  }
}

class IOrder extends Order {
  static final Logger logger = LoggerFactory.getLogger(IOrder.class);

  private static Set<String> twoArgWithBrackets = ImmutableSet.<String>builder()
      .add("lw", "sw", "lh", "lhu", "sh").build();
  private static Set<String> twoArg = ImmutableSet.<String>builder()
      .add("lui", "bgezal").build();
  private static Set<String> threeArg = ImmutableSet.<String>builder()
      .add("addi", "addiu", "andi", "ori", "xori").build();
  private static Set<String> threeArgWithLabel = ImmutableSet.<String>builder()
      .add("beq", "bne").build();
  private static Set<String> orders;

  static {
    Set<String> allTwoArg = Sets.union(twoArg, twoArgWithBrackets);
    Set<String> allThreeArg = Sets.union(threeArg, threeArgWithLabel);
    orders = Sets.union(allThreeArg, allTwoArg);
  }

  public static Boolean isIOrder(String orderName) {
    return orders.contains(orderName);
  }

  public static Integer transformOrder2Binary(String orderName, List<String> paraList, int PC,
      Map<String, Integer> map) {
    if (twoArgWithBrackets.contains(orderName) && paraList.size() == 2) {
      String rt = paraList.get(0);
      String tmp = paraList.get(1);
      int start = tmp.indexOf('(');
      int end = tmp.indexOf(')');
      if (start == -1 || end == -1 || end <= start) {
        return null;
      }
      String rs = tmp.substring(tmp.indexOf('(') + 1, tmp.indexOf(')'));
      String imm = tmp.substring(0, tmp.indexOf('('));
      if (imm.equals("")) {
        imm = "0";
      }
      if (Util.isNumber(imm) && registersMap.containsKey(rt) && registersMap.containsKey(rs)) {
        switch (orderName) {
          case "lw":
            return (35 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "sw":
            return (43 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "lh":
            return (33 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "lhu":
            return (37 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "sh":
            return (41 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
        }
      } else {
        return null;
      }
    }
    if (twoArg.contains(orderName) && paraList.size() == 2) {
      String rt = paraList.get(0);
      String imm = paraList.get(1);
      if (registersMap.containsKey(rt) && Util.isNumber(imm)) {
        switch (orderName) {
          case "lui":
            return (15 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF);
          case "bgezal":
            return (1 << 26) | ((17 & 31) << 16) | (registersMap.get(rt) << 21) | (
                ((Util.str2Num(imm) - PC) >> 2) & 0xFFFF);
        }
      } else {
        return null;
      }

    }
    if (threeArgWithLabel.contains(orderName) && paraList.size() == 3) {
      String rs = paraList.get(0);
      String rt = paraList.get(1);
      String label = paraList.get(2);
      Integer address;
      if (map.containsKey(label)) {
        address = map.get(label);
      }else if (Util.isNumber(label)){
        address = Util.str2Num(label);
      } else {
        return null;
      }
      if (registersMap.containsKey(rs) && registersMap.containsKey(rt)) {
        switch (orderName) {
          case "beq":
            return (4 << 26) | (registersMap.get(rs) << 21) | (registersMap.get(rt) << 16) | (
                ((address - PC) >> 2) & 0xFFFF);
          case "bne":
            return (5 << 26) | (registersMap.get(rs) << 21) | (registersMap.get(rt) << 16) | (
                ((address - PC) >> 2) & 0xFFFF);
        }
      } else {
        return null;
      }

    }
    if (threeArg.contains(orderName) && paraList.size() == 3) {
      String rt = paraList.get(0);
      String rs = paraList.get(1);
      String imm = paraList.get(2);
      if (registersMap.containsKey(rt) && registersMap.containsKey(rs) && Util.isNumber(imm)) {
        switch (orderName) {
          case "addi":
            return (8 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "addiu":
            return (9 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "andi":
            return (12 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "ori":
            return (13 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "xori":
            return (14 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
        }
      } else {
        return null;
      }

    }
    return null;
  }

}

class JOrder extends Order {
  static final Logger logger = LoggerFactory.getLogger(JOrder.class);

  static Set<String> orders = ImmutableSet.<String>builder().add("j", "jal").build();

  static Boolean isJOrder(String orderName) {
    return orders.contains(orderName);
  }

  static Integer transformOrder2Binary(String orderName, List<String> paraList,
      Map<String, Integer> map) {

    String label = paraList.get(0);
    int address;
    if (map.containsKey(label)) {
      address = map.get(label);
    } else if (Util.isNumber(label)) {
      address = Util.str2Num(label);
    } else {
      return null;
    }
    if (orderName.equals("j")) {
      return (2 << 26) | ((address) >> 1) & 0x3FFFFFF;
    } else if (orderName.equals("jal")) {
      return (3 << 26) | (address >> 1) & 0x3FFFFFF;
    } else {
      return null;
    }
  }
}


class Util {
  static final Logger logger = LoggerFactory.getLogger(Util.class);
  static Boolean isNumber(String str) {
    Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
    String bigStr;
    try {
      bigStr = new BigDecimal(str).toString();
    } catch (Exception e) {
      return false;
    }
    Matcher isNum = pattern.matcher(bigStr);
    if (!isNum.matches()) {
      return false;
    }
    return true;
  }

  static void printArray(String[] arr) {
    for (String s : arr) {
      logger.info(s+",");
    }
  }

  static int str2Num(String str) {
    return Integer.parseInt(str);
  }

  static List<String> preprocess(String str) {
    List<String> list = new ArrayList<>(Arrays.asList(str.split(",")));
    return list;
  }
}

import glimpseUtil.CommandLineTokenizer; 
public class TokenProbe { 
  public static void main(String[] args) { 
    String plain = "c:\\windows\\notepad.exe"; 
    String quoted = "\"c:\\windows\\notepad.exe\""; 
    System.out.println("plain=" + CommandLineTokenizer.tokenize(plain)); 
    System.out.println("quoted=" + CommandLineTokenizer.tokenize(quoted)); 
  } 
} 

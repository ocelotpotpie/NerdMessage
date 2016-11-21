package nu.nerd.nerdmessage;


import java.util.Arrays;

public class StringUtil {


    /**
     * Array join
     * @param separator A separator that goes between the array items. e.g. " " or ", "
     * @param arr The array to join
     * @return The joined string
     */
    public static String join(String separator, String[] arr) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : arr) {
            sb.append(s);
            if (i < (arr.length - 1)) {
                sb.append(separator);
            }
            i++;
        }
        return sb.toString().trim();
    }


    /**
     * Array join with default separator of a single space.
     */
    public static String join(String[] arr) {
        return join(" ", arr);
    }


    /**
     * Array join with an offset.
     * e.g. a start index of 2 means the array item at indices 0-1 will be skipped.
     * @param arr Array to join
     * @param start The index to start the join at
     */
    public static String join(String[] arr, int start) {
        return join(" ", Arrays.copyOfRange(arr, start, arr.length));
    }


    /**
     * Test a String for excessive capitalization.
     * Returns true if a string is over 50% caps and has more than one word.
     */
    public static boolean isAllCaps(String msg) {
        int caps = 0;
        int words = 1;
        if (msg.length() < 1) return false;
        for (int i=0; i<msg.length(); i++){
            if (Character.isUpperCase(msg.charAt(i))) caps++;
            if (Character.isSpaceChar(msg.charAt(i))) words++;
        }
        double percentage = (double) caps/msg.length();
        return ((words > 1) && (percentage > 0.5));
    }


    /**
     * Truncase a string to a given length and add an ellipsis if necessary
     * @param str the string to truncate
     * @param length the length in characters to truncate to
     * @return the revised string
     */
    public static String truncateEllipsis(String str, int length) {
        String newStr = str.substring(0, Math.min(str.length(), length));
        if (str.length() > length) {
            newStr = newStr + "...";
        }
        return newStr;
    }


}

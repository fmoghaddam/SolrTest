
public class StringUtils
{

    /**
     *  Converts a string from the Unicode representation into something that can be
     *  embedded in a java properties file.  All references outside the ASCII range
     *  are replaced with \\uXXXX.
     *
     *  @param s The string to convert
     *  @return the ASCII string
     */
    public static String native2Ascii(String s)
    {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < s.length(); i++)
        {
            char aChar = s.charAt(i);
            if ((aChar < 0x0020) || (aChar > 0x007e))
            {
                sb.append('\\');
                sb.append('u');
                sb.append(toHex((aChar >> 12) & 0xF));
                sb.append(toHex((aChar >>  8) & 0xF));
                sb.append(toHex((aChar >>  4) & 0xF));
                sb.append(toHex( aChar        & 0xF));
            }
            else if(aChar=='.'){
            	sb.append('\\');
                sb.append('u');
                sb.append('0');
                sb.append('0');
                sb.append('2');
                sb.append('e');
            }
            else
            {
                sb.append(aChar);
            }
        }
        return sb.toString();
    }
    private static char toHex(int nibble)
    {
        final char[] hexDigit =
                {
                        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
                };
        return hexDigit[nibble & 0xF];
    }


}
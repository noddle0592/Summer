package ren.yale.java.tools;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yale
 * create at: 2018-02-06 17:41
 **/
public class CommandLineUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(CommandLineUtils.class.getName());
    private String[]args;
    private  CommandLineParser parser =null;
    private CommandLine cmd = null;
    private final Options options = new Options();
    public CommandLineUtils(String []args){
        this.args = args;
    }
    public CommandLineUtils addOption(String opt, boolean hasArg, String description){
        Option option = new Option(opt, hasArg,description);
        options.addOption(option).addOption(option);
        return this;
    }

    //array parameters : [-a,av,-b,bv]
    public CommandLineUtils basicParse(){
        try {
            parser = new BasicParser();
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            LOGGER.error(e.getMessage());
        }

        return this;
    }
    //Posix style parameters : -a av -b bv
    public CommandLineUtils posixParse(){
        try {
            parser = new PosixParser();
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            LOGGER.error(e.getMessage());
        }

        return this;
    }
    //GNU style parameters : --a=av --b=bv
    public CommandLineUtils gnuParse(){
        try {
            parser = new GnuParser();
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            LOGGER.error(e.getMessage());
        }

        return this;
    }
    public static CommandLineUtils args(String []args){
        return new CommandLineUtils(args);
    }

    public int getIntValue(String key,int defaultV){

        if (cmd !=null&&cmd.hasOption(key)){
            String v = cmd.getOptionValue(key);
            try {
                return Integer.valueOf(v);
            }catch (Exception e){
                LOGGER.error(e.getMessage());
            }
        }
        return defaultV;
    }

    public String getStrValue(String key,String defaultV){

        if (cmd !=null&&cmd.hasOption(key)){
            String v = cmd.getOptionValue(key);
            if (v==null||v.length()==0){
                return defaultV;
            }
            return v;
        }
        return defaultV;
    }






}

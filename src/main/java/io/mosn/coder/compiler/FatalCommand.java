package io.mosn.coder.compiler;

public class FatalCommand extends Command {

    public FatalCommand(){
        super();
        this.fastQuit =true;
        this.fatalQuit = true;
    }

}

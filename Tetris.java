import java.util.*;
import java.util.concurrent.TransferQueue;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.time.Clock;

public class Tetris {
    Board board;//面板
    ArrayList next;//next块的序列
    Blocks nowBlock;//当前块
    Blocks holdBlock;//hold块
    int x,y,index;//xy坐标，index代表方向
    int shadow_y;
    long softDropTime;//按压时间
    int dropBlockTimer;//落块计时器
    int lockBlockTimer;//锁定计时器
    boolean lose;
    int nextCount=5;//能看多少个next
    int holdCount=0;//一个块被放下前只能hold一次
    int x0=3,y0=23;//初始xy坐标
    int interval=60; 
    JFrame frame=new JFrame("Tetris");
    MoveHandler kbHandler=new MoveHandler();
    SpinHandler spHandler=new SpinHandler();
    MainPanel mainPanel;//游戏主画面，用来画board
    HoldPanel holdPanel;//hold面板，用来画hold的块
    NextPanel nextPanel;//next面板，用来画next序列
    int lockTime=1000;//落下时的锁定时间
    boolean isTSpin;
    boolean isTSpinMini;
    Tetris(){
        reset();
    }

    private class MoveHandler implements KeyListener {
        public void keyTyped(KeyEvent e) {}

        public void keyPressed(KeyEvent e) {
            // TODO Auto-generated method stub
            if(!lose){
                switch(e.getKeyCode()){
                    case KeyEvent.VK_A:
                        if(board.canBePutted(nowBlock, x-1, y, index)){
                            --x;
                            resetTSpin();
                            paintChanges();
                        }
                    break;
                    case KeyEvent.VK_D:
                        if(board.canBePutted(nowBlock, x+1, y, index)){
                            ++x;
                            resetTSpin();
                            paintChanges();
                        }
                    break;
                    case KeyEvent.VK_W:
                        y=shadow_y;
                        changeBlock();
                        paintChanges();
                    break;
                    case KeyEvent.VK_S:
                        if(softDropTime==0){
                            ++softDropTime;
                        }
                    break;
                    case KeyEvent.VK_R:
                        reset();
                        paintChanges();
                    break;
                    case KeyEvent.VK_SHIFT:
                        if(holdCount==0){
                            if(holdBlock==null){
                                holdBlock=nowBlock;
                                nowBlock=(Blocks)next.get(0);
                                next.remove(0);
                                if(next.size()<=nextCount){
                                    creatBlocks();
                                }
                            }
                            else{
                                Blocks tempBlock=nowBlock;
                                nowBlock=holdBlock;
                                holdBlock=tempBlock;
                            }
                            x=x0;
                            y=y0;
                            index=0;
                            ++holdCount;
                            paintChanges();
                        }
                    break;
                }
            }
            else if(e.getKeyCode()==KeyEvent.VK_R){
                reset();
                paintChanges();
            }
        }

        public void keyReleased(KeyEvent e) {
            // TODO Auto-generated method stub
            if(e.getKeyCode()==KeyEvent.VK_S){
                softDropTime=0;
            }

        }
    }
    private class SpinHandler implements KeyListener{

        public void keyTyped(KeyEvent e) {
            // TODO Auto-generated method stub

        }

        public void keyPressed(KeyEvent e) {
            // TODO Auto-generated method stub
            int[][] temp;
            switch(e.getKeyCode()){
                case KeyEvent.VK_LEFT:
                    if(nowBlock.equals(Blocks.I)){
                        temp=SRS.iBlock[index][(index+3)%4];
                    }
                    else{
                        temp=SRS.otherBlocks[index][(index+3)%4];
                    }
                    for(int i=0;i<5;++i){
                        if(board.canBePutted(nowBlock,x+temp[i][0],y+temp[i][1],(index+3)%4)){
                            if(nowBlock.equals(Blocks.T)){
                                isTSpinMini=isTSpinMini(i);
                                if(!isTSpinMini){
                                    isTSpin=isTSpin(i);
                                }
                            }
                            index=(index+3)%4;
                            x+=temp[i][0];
                            y+=temp[i][1];
                            paintChanges();
                            break;
                        }
                    }
                break;
                case KeyEvent.VK_RIGHT:
                    if(nowBlock.equals(Blocks.I)){
                        temp=SRS.iBlock[index][(index+1)%4];
                    }
                    else{
                        temp=SRS.otherBlocks[index][(index+1)%4];
                    }
                    for(int i=0;i<5;++i){
                        if(board.canBePutted(nowBlock,x+temp[i][0],y+temp[i][1],(index+1)%4)){
                            if(nowBlock.equals(Blocks.T)){
                                isTSpinMini=isTSpinMini(i);
                                if(!isTSpinMini){
                                    isTSpin=isTSpin(i);
                                }
                            }
                            index=(index+1)%4;
                            x+=temp[i][0];
                            y+=temp[i][1];
                            paintChanges();
                            break;
                        }
                    }
                break;
            }

        }

        public void keyReleased(KeyEvent e) {
            // TODO Auto-generated method stub

        }
        
    }
    //生成7bag的块
    private void creatBlocks(){
        Blocks[] bag={Blocks.I,Blocks.L,Blocks.J,Blocks.O,Blocks.S,Blocks.Z,Blocks.T};
        Random random=new Random();
        while(true){
            int r=random.nextInt(7);
            if(bag[r]!=null){
                next.add(bag[r]);
                bag[r]=null;
            }
            for(r=0;r<7;++r){
                if(bag[r]!=null){
                    break;
                }
            }
            if(r==7){
                break;
            }
        }
    }
    //开始游戏,唯一对外接口
    public void play(){
        setGUI();
        while(!lose){
            if(dropBlockTimer>=interval){
                dropBlockTimer=0;
                mainPart();
            }
            if(softDropTime!=0){
                if((softDropTime-1)%6==0&&softDropTime!=0){
                    dropBlockTimer=0;
                    mainPart();
                }
                ++softDropTime;
            }
            else{
                ++dropBlockTimer;
            }
            if(board.canBePutted(nowBlock, x, y+1, index)){
                lockBlockTimer=0;
            }
            else{
                ++lockBlockTimer;
            }
            try{
                Thread.sleep((long)(1000/60));
            }catch(Exception e){}
        }
    }
    //画图函数
    private void paintChanges(){
        shadow();
        mainPanel.board=board;
        mainPanel.nowBlock=nowBlock;
        mainPanel.block_x=x;
        mainPanel.block_y=y;
        mainPanel.shadow_y=shadow_y;
        mainPanel.blockIndex=index;
        holdPanel.block=holdBlock;
        nextPanel.next=next;
        mainPanel.repaint();
        nextPanel.repaint();
        holdPanel.repaint();
    }
    //换成下一块
    private void changeBlock(){
        board.isTSpin=isTSpin;
        board.isTSpinMini=isTSpinMini;
        board.addBlock(nowBlock, x, y, index);
        nowBlock=(Blocks)next.get(0);
        next.remove(0);
        if(next.size()<=nextCount){
            creatBlocks();
        }
        x=x0;
        y=y0;
        holdCount=0;
        index=0;
        resetTSpin();
        if((!board.canBePutted(nowBlock, x, y, index))){
            lose=true;
        }
    }
    //处理影子
    private void shadow(){
        shadow_y=y;
        while(board.canBePutted(nowBlock, x, shadow_y+1, index)){
            ++shadow_y;
        }
    }
    //主要下落逻辑
    private void mainPart(){
        if(board.canBePutted(nowBlock, x, y+1, index)){
            y++;
            resetTSpin();
        }
        else{
            if(lockBlockTimer*1000/60>=lockTime){
                changeBlock();
            }
        }
        paintChanges();
    }
    //GUI
    private void setGUI(){
        mainPanel=new MainPanel(board,nowBlock,x,y,index,shadow_y);
        holdPanel=new HoldPanel(holdBlock);
        nextPanel=new NextPanel(next,nextCount);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addKeyListener(kbHandler);
        frame.addKeyListener(spHandler);
        mainPanel.setSize(200, 400);
        holdPanel.setSize(80,80);
        nextPanel.setSize(80,400);
        JPanel borderPanel0=new BorderPanel();
        borderPanel0.setSize(20, 400);
        JPanel borderPanel1=new BorderPanel();
        borderPanel1.setSize(20, 400);
        frame.add(holdPanel);
        frame.add(mainPanel);
        frame.add(nextPanel);
        frame.add(borderPanel0);
        frame.add(borderPanel1);
        frame.setLayout(null);
        mainPanel.setLocation(100,0);
        holdPanel.setLocation(0,0);
        nextPanel.setLocation(320,0);
        borderPanel0.setLocation(80, 0);
        borderPanel1.setLocation(300,0);
        frame.setSize(420, 440);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    //重置游戏
    private void reset(){
        board = new Board();
        next=new ArrayList<Blocks>();
        creatBlocks();
        nowBlock=(Blocks)next.get(0);
        holdBlock=null;
        next.remove(0);
        x=x0;
        y=y0;
        index=0;
        shadow();
        softDropTime=0;
        dropBlockTimer=0;
        lockBlockTimer=0;
        lose=false;
        resetTSpin();
    }
    //是不是T-Spin或T-SpinMini
    private boolean isTSpin(int i){
        if(i!=0){
            return true;
        }
        int temp=0;
        if(board.board[x][y]!=0){
            ++temp;
        }
        if(x+2<10){
            if(board.board[x+2][y]!=0){
                ++temp;
            }
        }
        else{
            ++temp;
        }
        if(y+2<44){
            if(board.board[x][y+2]!=0){
                ++temp;
            }
        }
        else{
            ++temp;
        }
        if(x+2<10&&y+2<44){
            if(board.board[x+2][y+2]!=0){
                ++temp;
            }
        }
        else{
            ++temp;
        }
        if(temp>=3){
            return true;
        }
        return false;
    }
    private boolean isTSpinMini(int i){
        if((index==1||index==3)&&i==2){
            return true;
        }
        if(index==0&&i==1){
            return true;
        }
        return false;
    }
    //重置T-Spin
    private void resetTSpin(){
        isTSpin=false;
        isTSpinMini=false;
    }
    public static void main(String[] args){
        new Tetris().play();
    }
}
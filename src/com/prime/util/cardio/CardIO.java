package com.prime.util.cardio;

import javax.smartcardio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CardIO {

    private static List<ICardListener> cardListeners;
    private static CardIO defaultInstance;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private List<CardTerminal> cardTerminals;


    static {
        defaultInstance = new CardIO();
    }

    public interface ICardListener{
        void onCardInserted(CardTerminal cardTerminal);
        void onCardEjected(CardTerminal cardTerminal);
        void onDeviceDetected(List<CardTerminal> cardTerminals);
        void onDeviceDetached(CardTerminal cardTerminal);
    }


    private CardIO(){
        cardListeners = new ArrayList<>();
        cardTerminals = new ArrayList<>();
        startDeviceDetectDemon();
    }


    private void startDeviceDetectDemon(){
        Thread thread = new Thread(()->{
            while(true){
                final List<CardTerminal> oldCardTerminalList = cardTerminals, newList;
                cardTerminals = scan();
                newList = cardTerminals.stream().filter(ct->!oldCardTerminalList.contains(ct))
                        .collect(Collectors.toList());
                if(newList.size()>0) {
                    cardListeners.forEach(l->l.onDeviceDetected(newList));
                    newList.forEach(this::startCardStateChangedDaemon);
                }
                try{ Thread.sleep(1000); } catch (InterruptedException ie){ie.printStackTrace();}
            }
        }, "DeviceDetectThread");

        executorService.submit(thread);
    }


    private List<CardTerminal> scan(){
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            cardTerminals = factory.terminals().list();
            return cardTerminals;
        } catch(CardException ce){ return new ArrayList<>(); }
    }


    private void startCardStateChangedDaemon(CardTerminal cardTerminal){
        Thread thread = new Thread(()-> {
            while (true) {
                try{
                     while(!cardTerminal.waitForCardPresent(500)) continue;
                    cardListeners.forEach(l->l.onCardInserted(cardTerminal));
                    while(!cardTerminal.waitForCardAbsent(500)) continue;
                    cardListeners.forEach(l->l.onCardEjected(cardTerminal));
                } catch(Exception e){
                    System.out.println("device removed"); //Exception thrown by cardTerminal.waitForCardPresent or Ejected() instructs that the terminal is detached
                    cardListeners.forEach(l->l.onDeviceDetached(cardTerminal));
                    return;
                }
            }
        }, "CardDetectThread");
        executorService.submit(thread);
    }

    /*
    private void startCardPresentDetectDemon(CardTerminal cardTerminal){
        Thread thread = new Thread(()->{
            System.out.println("thread count " + threadCount);
            try {
                while (!cardTerminal.isCardPresent()) Thread.sleep(500);
                cardListeners.forEach(l -> l.onCardInserted(cardTerminal));
                startCardAbsentDetectDemon(cardTerminal);
            }
            catch(InterruptedException ie){} //probably due to another refresh() call
            catch (Exception e){
                System.out.println("device removed"); //Exception thrown by cardTerminal.isCardPresent() instructs that the terminal is detached
                cardListeners.forEach(l->l.onDeviceDetached(cardTerminal));
            }
            return;
        }, "CardPresentDetectThread");

        executorService.submit(thread); //add to list and start
    }


    private void startCardAbsentDetectDemon(CardTerminal cardTerminal){
        Thread thread = new Thread(()->{
            try{
                while (cardTerminal.isCardPresent()) Thread.sleep(500);
                cardListeners.forEach(l->l.onCardEjected(cardTerminal));
                startCardPresentDetectDemon(cardTerminal);
            }
            catch (InterruptedException ie){} //probably due to a refresh() call
            catch (Exception e){
                System.out.println("device removed");  //Exception thrown by cardTerminal.isCardPresent() instructs that the terminal is detached
                cardListeners.forEach(l->l.onDeviceDetached(cardTerminal));
            }
            return;
        }, "CardAbsentDetectThread");
        thread.start();

        executorService.submit(thread); //add to list and start
    }
    */

    public Card connect(CardTerminal cardTerminal) throws CardException{
        return cardTerminal.connect("*");
    }


    public List<CardTerminal> getTerminals(){
        return cardTerminals;
    }


    public static CardIO getInstance(){
        return defaultInstance;
    }


    public List<CardTerminal> refresh(){
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            cardTerminals = factory.terminals().list();
            executorService.shutdownNow();
            executorService = Executors.newCachedThreadPool();
            cardTerminals.forEach(this::startCardStateChangedDaemon);
            return cardTerminals;
        } catch(CardException ce){ return new ArrayList<>(); }
    }


    public ResponseAPDU transmit(CardChannel cardChannel, byte[] data) throws CardException{
        return cardChannel.transmit(new CommandAPDU(data));
    }



    public void addListener(ICardListener cardListener){
        cardListeners.add(cardListener);
    }
}

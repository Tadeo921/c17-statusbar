package org.hapjs.card.sdk;
public final class CardDelegator implements org.hapjs.card.api.Card {
    private Object mCard;private org.hapjs.card.api.CardService cardService;
    public CardDelegator(Object card,org.hapjs.card.api.CardService service){mCard=card;cardService=service;}
    public Object getView(){throw new AssertionError("native getView may addView; diagnostic is fields only");}
}

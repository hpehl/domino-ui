package org.dominokit.domino.ui.forms;

import elemental2.dom.*;
import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.dropdown.DropDownPosition;
import org.dominokit.domino.ui.dropdown.DropdownAction;
import org.dominokit.domino.ui.keyboard.KeyboardEvents;
import org.dominokit.domino.ui.loaders.Loader;
import org.dominokit.domino.ui.loaders.LoaderEffect;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.utils.DelayedTextInput;
import org.dominokit.domino.ui.utils.HasSelectionHandler;
import org.jboss.elemento.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static elemental2.dom.DomGlobal.document;
import static elemental2.dom.DomGlobal.window;
import static java.util.Objects.nonNull;
import static org.dominokit.domino.ui.style.Unit.px;
import static org.jboss.elemento.Elements.div;

public class SuggestBox<T> extends AbstractValueBox<SuggestBox<T>, HTMLInputElement, T> implements HasSelectionHandler<SuggestBox<T>, SuggestItem<T>> {

    private static final String TEXT = "text";
    private DropDownMenu suggestionsMenu;
    private List<SelectionHandler<SuggestItem<T>>> selectionHandlers = new ArrayList<>();
    private SuggestBoxStore<T> store;
    private HTMLDivElement loaderContainer = div().css("suggest-box-loader").element();
    private Loader loader;
    private boolean emptyAsNull;
    private Color highlightColor;
    private T value;
    private int typeAheadDelay = 200;
    private DelayedTextInput delayedTextInput;
    private DelayedTextInput.DelayedAction delayedAction = () -> {
        if (isEmpty()) {
            suggestionsMenu.close();
        } else {
            search();
        }
    };

    public SuggestBox() {
        this("");
    }

    public SuggestBox(String label) {
        this(label, null);
    }

    public SuggestBox(SuggestBoxStore<T> store) {
        this("", store);
    }

    public SuggestBox(String label, SuggestBoxStore<T> store) {
        this(TEXT, label, store);
    }

    public SuggestBox(String type, String label, SuggestBoxStore<T> store) {
        super(type, label);
        this.store = store;
        suggestionsMenu = DropDownMenu.create(fieldContainer);
        suggestionsMenu.setAppendTarget(fieldContainer.element());
        suggestionsMenu.setAppendStrategy(DropDownMenu.AppendStrategy.FIRST);
        suggestionsMenu.setPosition(new PopupPositionTopDown());
        suggestionsMenu.addCloseHandler(this::focus);
        Element element = document.querySelector(".content");
        if (nonNull(element)) {
            element.addEventListener("transitionend", evt -> {
                suggestionsMenu.style().setWidth(element().offsetWidth + "px");
            });
        }
        onAttached(mutationRecord -> {
            suggestionsMenu.style().setWidth(element().offsetWidth + "px");
        });
        getFieldContainer().insertFirst(loaderContainer);
        setLoaderEffect(LoaderEffect.IOS);

        delayedTextInput = DelayedTextInput.create(getInputElement(), typeAheadDelay)
                .setDelayedAction(delayedAction);
        KeyboardEvents.listenOn(getInputElement())
                .onArrowDown(evt -> {
                    suggestionsMenu.focus();
                    evt.preventDefault();
                })
                .onArrowUp(evt -> {
                    suggestionsMenu.focus();
                    evt.preventDefault();
                })
                .onEscape(evt -> {
                    focus();
                    evt.preventDefault();
                });
    }

    public static <T> SuggestBox<T> create(SuggestBoxStore<T> store) {
        return new SuggestBox<>(store);
    }

    public static <T> SuggestBox<T> create(String label, SuggestBoxStore<T> store) {
        return new SuggestBox<T>(label, store);
    }

    private void search() {
        if (store != null) {
            loader.start();
            suggestionsMenu.clearActions();
            suggestionsMenu.close();
            store.filter(getStringValue(), suggestions -> {
                suggestionsMenu.clearActions();
                suggestions.forEach(suggestion -> {
                    suggestion.highlight(SuggestBox.this.getStringValue(), highlightColor);
                    suggestionsMenu.appendChild(dropdownAction(suggestion));
                });
                suggestionsMenu.open();
                loader.stop();
            });
        }
    }

    @Override
    protected HTMLInputElement createInputElement(String type) {
        return Elements.input(type).element();
    }

    public int getTypeAheadDelay() {
        return typeAheadDelay;
    }

    public SuggestBox<T> setTypeAheadDelay(int delayMilliseconds) {
        this.typeAheadDelay = delayMilliseconds;
        this.delayedTextInput.setDelay(delayMilliseconds);
        return this;
    }

    public DelayedTextInput.DelayedAction getDelayedAction() {
        return delayedAction;
    }

    public SuggestBox<T> setDelayedAction(DelayedTextInput.DelayedAction delayedAction) {
        this.delayedAction = delayedAction;
        this.delayedTextInput.setDelayedAction(delayedAction);
        return this;
    }

    public SuggestBox<T> setOnEnterAction(DelayedTextInput.DelayedAction onEnterAction) {
        this.delayedTextInput.setOnEnterAction(onEnterAction);
        return this;
    }

    @Override
    protected void clearValue() {
        value(null);
    }

    @Override
    protected void doSetValue(T value) {
        if (nonNull(store)) {
            store.find(value, suggestItem -> {
                if (nonNull(suggestItem)) {
                    this.value = value;
                    getInputElement().element().value = suggestItem.getDisplayValue();
                } else {
                    SuggestBoxStore.MissingSuggestProvider<T> messingSuggestionProvider = store.getMessingSuggestionProvider();
                    Optional<SuggestItem<T>> messingSuggestion = messingSuggestionProvider.getMessingSuggestion(value);
                    if (messingSuggestion.isPresent()) {
                        SuggestItem<T> messingSuggestItem = messingSuggestion.get();
                        this.value = messingSuggestItem.getValue();
                        getInputElement().element().value = messingSuggestItem.getDisplayValue();
                    } else {
                        this.value = null;
                        getInputElement().element().value = "";
                    }
                }
            });
        }
    }

    @Override
    public T getValue() {
        return this.value;
    }

    public SuggestBox<T> setSuggestBoxStore(SuggestBoxStore<T> store) {
        this.store = store;
        return this;
    }

    public SuggestBox<T> setType(String type) {
        getInputElement().element().type = type;
        return this;
    }

    @Override
    public String getStringValue() {
        String stringValue = getInputElement().element().value;
        if (stringValue.isEmpty() && isEmptyAsNull()) {
            return null;
        }
        return stringValue;
    }

    private DropdownAction<T> dropdownAction(SuggestItem<T> suggestItem) {
        DropdownAction<T> dropdownAction = suggestItem.asDropDownAction();
        dropdownAction.addSelectionHandler(value -> {
            setValue(value);
            selectionHandlers.forEach(handler -> handler.onSelection(suggestItem));
            suggestionsMenu.close();
        });
        return dropdownAction;
    }

    @Override
    public SuggestBox<T> addSelectionHandler(SelectionHandler<SuggestItem<T>> selectionHandler) {
        selectionHandlers.add(selectionHandler);
        return this;
    }

    @Override
    public SuggestBox<T> removeSelectionHandler(SelectionHandler<SuggestItem<T>> selectionHandler) {
        selectionHandlers.remove(selectionHandler);
        return this;
    }

    public SuggestBox<T> setLoaderEffect(LoaderEffect loaderEffect) {
        loader = Loader.create(loaderContainer, loaderEffect)
                .setSize("20px", "20px")
                .setRemoveLoadingText(true);
        return this;
    }

    public Loader getLoader() {
        return loader;
    }

    public SuggestBox<T> setEmptyAsNull(boolean emptyAsNull) {
        this.emptyAsNull = emptyAsNull;
        return this;
    }

    public boolean isEmptyAsNull() {
        return emptyAsNull;
    }

    public SuggestBoxStore<T> getStore() {
        return store;
    }

    public DelayedTextInput getDelayedTextInput() {
        return delayedTextInput;
    }

    public DropDownMenu getSuggestionsMenu() {
        return suggestionsMenu;
    }

    public SuggestBox<T> setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
        return this;
    }

    @Override
    protected AutoValidator createAutoValidator(AutoValidate autoValidate) {
        return new SuggestAutoValidator<>(this, autoValidate);
    }

    public static class PopupPositionTopDown implements DropDownPosition {

        private DropDownPositionUp up = new DropDownPositionUp();
        private DropDownPositionDown down = new DropDownPositionDown();

        @Override
        public void position(HTMLElement popup, HTMLElement target) {
            ClientRect targetRect = target.getBoundingClientRect();

            double distanceToMiddle = ((targetRect.top) - (targetRect.height / 2));
            double windowMiddle = DomGlobal.window.innerHeight / 2;

            if (distanceToMiddle >= windowMiddle) {
                up.position(popup, target);
                popup.setAttribute("popup-direction", "top");
            } else {
                down.position(popup, target);
                popup.setAttribute("popup-direction", "down");
            }

            popup.style.setProperty("width", targetRect.width + "px");
        }
    }

    public static class DropDownPositionUp implements DropDownPosition {
        @Override
        public void position(HTMLElement actionsMenu, HTMLElement target) {
            ClientRect targetRect = target.getBoundingClientRect();

            actionsMenu.style.setProperty("bottom", px.of(((window.innerHeight - targetRect.bottom) - window.pageYOffset + targetRect.height + 5)));
            actionsMenu.style.setProperty("left", px.of((targetRect.left + window.pageXOffset)));
            actionsMenu.style.removeProperty("top");
        }
    }

    public static class DropDownPositionDown implements DropDownPosition {
        @Override
        public void position(HTMLElement actionsMenu, HTMLElement target) {
            ClientRect targetRect = target.getBoundingClientRect();
            actionsMenu.style.setProperty("top", px.of((targetRect.top + window.pageYOffset + targetRect.height)));
            actionsMenu.style.setProperty("left", px.of((targetRect.left + window.pageXOffset)));
            actionsMenu.style.removeProperty("bottom");
        }
    }

    private static class SuggestAutoValidator<T> extends AutoValidator {

        private SuggestBox<T> suggestBox;
        private SelectionHandler<SuggestItem<T>> selectionHandler;

        public SuggestAutoValidator(SuggestBox<T> suggestBox, AutoValidate autoValidate) {
            super(autoValidate);
            this.suggestBox = suggestBox;
        }

        @Override
        public void attach() {
            selectionHandler = option -> autoValidate.apply();
            suggestBox.addSelectionHandler(selectionHandler);
        }

        @Override
        public void remove() {
            suggestBox.removeSelectionHandler(selectionHandler);
        }
    }
}

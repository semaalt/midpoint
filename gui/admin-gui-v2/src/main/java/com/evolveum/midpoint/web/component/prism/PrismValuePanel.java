/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismValuePanel extends Panel {

    private IModel<ValueWrapper> model;

    public PrismValuePanel(String id, IModel<ValueWrapper> model) {
        super(id);
        Validate.notNull(model, "Property value model must not be null.");
        this.model = model;

        add(new AttributeAppender("class", new Model<String>("objectFormValue"), " "));

        initLayout();
    }

    private void initLayout() {
        //feedback
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

//        //helpButton
//        StaticImage helpButtonImage = new StaticImage("helpButton", new Model<String>("../../img/formIcon/InfoSmall.png"));
//        helpButtonImage.setOutputMarkupId(true);
//        add(helpButtonImage);
//
//        //helpContent
//        Label labelHelpContent = new Label("helpContent", "This is help text sample...");
//        labelHelpContent.setMarkupId("content_" + helpButtonImage.getMarkupId());
//        add(labelHelpContent);

        //input
        InputPanel input = createInputComponent("input", feedback);
        add(input);

        feedback.setFilter(new ComponentFeedbackMessageFilter(input.getComponent()));

        //buttons
        AjaxLink addButton = new AjaxLink("addButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        };
        addButton.add(new Image("addIcon", new PackageResourceReference(PrismValuePanel.class, "AddSmall.png")));
        addButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible();
            }
        });
        add(addButton);

        AjaxLink removeButton = new AjaxLink("removeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValue(target);
            }
        };
        removeButton.add(new Image("removeIcon", new PackageResourceReference(PrismValuePanel.class, "DeleteSmall.png")));
        removeButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        add(removeButton);
    }

    private int countUsableValues(PropertyWrapper property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }

            if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
                continue;
            }

            count++;
        }
        return count;
    }

    private int countNonDeletedValues(PropertyWrapper property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean hasEmptyPlaceholder(PropertyWrapper property) {
        for (ValueWrapper value : property.getValues()) {
            if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
                return true;
            }
        }

        return false;
    }

    private boolean isRemoveButtonVisible() {
        ValueWrapper valueWrapper = model.getObject();
        PropertyWrapper propertyWrapper = valueWrapper.getProperty();
        PrismPropertyDefinition definition = propertyWrapper.getProperty().getDefinition();
        int min = definition.getMinOccurs();

        int count = countNonDeletedValues(propertyWrapper);
        if (count <= 1 || count <= min) {
            return false;
        }

        return true;
    }

    private boolean isAddButtonVisible() {
        ValueWrapper valueWrapper = model.getObject();
        PropertyWrapper propertyWrapper = valueWrapper.getProperty();
        PrismProperty property = propertyWrapper.getProperty();

        PrismPropertyDefinition definition = property.getDefinition();
        int max = definition.getMaxOccurs();
        if (max == -1) {
            return true;
        }

        if (countNonDeletedValues(propertyWrapper) >= max) {
            return false;
        }

        return true;
    }

    private InputPanel createInputComponent(String id, final FeedbackPanel feedback) {
        //todo create input components
        InputPanel component = createTypedInputComponent(id);

        final FormComponent formComponent = component.getComponent();
        if (formComponent instanceof TextField) {
            formComponent.add(new AttributeModifier("size", "42"));
        }
        formComponent.add(new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(formComponent);
                target.add(feedback);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                target.add(formComponent);
                target.add(feedback);

                super.onError(target, e);
            }
        });
        return component;
    }

    private InputPanel createTypedInputComponent(String id) {
        PrismProperty property = model.getObject().getProperty().getProperty();
        QName valueType = property.getDefinition().getTypeName();

        InputPanel panel;
        if (DOMUtil.XSD_DATETIME.equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(model, "value.value"));
//        } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
//            panel = new PasswordPanel(id, new PropertyModel<String>(model, "value.value"));
        } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
            panel = new CheckPanel(id, new PropertyModel<Boolean>(model, "value.value"));
        } else {
            Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
            if (type != null && type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            panel = new TextPanel<String>(id, new PropertyModel<String>(model, "value.value"),
                    type);
        }

        return panel;
    }

    private void addValue(AjaxRequestTarget target) {
        ValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();

        List<ValueWrapper> values = propertyWrapper.getValues();
        values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

    private void removeValue(AjaxRequestTarget target) {
        ValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();

        List<ValueWrapper> values = propertyWrapper.getValues();

        switch (wrapper.getStatus()) {
            case ADDED:
                values.remove(wrapper);
                break;
            case DELETED:
                throw new IllegalStateException("Couldn't delete already deleted item: " + toString());
            case NOT_CHANGED:
                wrapper.setStatus(ValueStatus.DELETED);
                break;
        }

        int count = countUsableValues(propertyWrapper);
        if (count == 0 && !hasEmptyPlaceholder(propertyWrapper)) {
            values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));
        }

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

//    public class StaticImage extends WebComponent {
//        private static final long serialVersionUID = 1L;
//
//        public StaticImage(String id, IModel<String> model) {
//            super(id, model);
//        }
//
//        protected void onComponentTag(ComponentTag tag) {
//            super.onComponentTag(tag);
//            checkComponentTag(tag, "img");
//            tag.put("src", getDefaultModelObjectAsString());
//            tag.put("alt", "");
//        }
//
//    }
}

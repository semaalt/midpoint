/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNodeImpl;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class LexicalUtils {

	@NotNull
	public static RootXNodeImpl createRootXNode(XNodeImpl xnode, QName rootElementName) {
		if (xnode instanceof RootXNodeImpl) {
			return (RootXNodeImpl) xnode;
		} else {
			RootXNodeImpl xroot = new RootXNodeImpl(rootElementName);
			xroot.setSubnode(xnode);
			return xroot;
		}
	}
}
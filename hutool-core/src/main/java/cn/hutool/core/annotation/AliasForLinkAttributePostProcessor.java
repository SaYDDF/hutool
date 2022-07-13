package cn.hutool.core.annotation;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * 处理注解中带有{@link Link}注解，且{@link Link#type()}为{@link RelationType#FORCE_ALIAS_FOR}
 * 或{@link RelationType#ALIAS_FOR}的属性。
 *
 * @author huangchengxing
 * @see ForceAliasedAnnotationAttribute
 * @see AliasedAnnotationAttribute
 */
public class AliasForLinkAttributePostProcessor implements SynthesizedAnnotationPostProcessor {

	@Override
	public int order() {
		return Integer.MIN_VALUE + 2;
	}

	@Override
	public void process(SynthesizedAnnotation annotation, SynthesizedAnnotationAggregator synthesizedAnnotationAggregator) {
		final Map<String, AnnotationAttribute> attributeMap = new HashMap<>(annotation.getAttributes());
		attributeMap.forEach((originalAttributeName, originalAttribute) -> {
			// 获取注解
			final Link link = SyntheticAnnotationUtil.getLink(
				originalAttribute, RelationType.ALIAS_FOR, RelationType.FORCE_ALIAS_FOR
			);
			if (ObjectUtil.isNull(link)) {
				return;
			}

			// 获取注解属性
			final SynthesizedAnnotation aliasAnnotation = SyntheticAnnotationUtil.getLinkedAnnotation(link, synthesizedAnnotationAggregator, annotation.annotationType());
			if (ObjectUtil.isNull(aliasAnnotation)) {
				return;
			}
			final AnnotationAttribute aliasAttribute = aliasAnnotation.getAttributes().get(link.attribute());
			SyntheticAnnotationUtil.checkLinkedAttributeNotNull(originalAttribute, aliasAttribute, link);
			SyntheticAnnotationUtil.checkAttributeType(originalAttribute, aliasAttribute);
			checkCircularDependency(originalAttribute, aliasAttribute);

			// aliasFor
			if (RelationType.ALIAS_FOR.equals(link.type())) {
				wrappingLinkedAttribute(synthesizedAnnotationAggregator, originalAttribute, aliasAttribute, AliasedAnnotationAttribute::new);
				return;
			}
			// forceAliasFor
			wrappingLinkedAttribute(synthesizedAnnotationAggregator, originalAttribute, aliasAttribute, ForceAliasedAnnotationAttribute::new);
		});
	}

	/**
	 * 对指定注解属性进行包装，若该属性已被包装过，则递归以其为根节点的树结构，对树上全部的叶子节点进行包装
	 */
	private void wrappingLinkedAttribute(
		SynthesizedAnnotationAggregator synthesizedAnnotationAggregator, AnnotationAttribute originalAttribute, AnnotationAttribute aliasAttribute, BinaryOperator<AnnotationAttribute> wrapping) {
		// 不是包装属性
		if (!aliasAttribute.isWrapped()) {
			processAttribute(synthesizedAnnotationAggregator, originalAttribute, aliasAttribute, wrapping);
			return;
		}
		// 是包装属性
		final AbstractWrappedAnnotationAttribute wrapper = (AbstractWrappedAnnotationAttribute)aliasAttribute;
		wrapper.getAllLinkedNonWrappedAttributes().forEach(
			t -> processAttribute(synthesizedAnnotationAggregator, originalAttribute, t, wrapping)
		);
	}

	/**
	 * 获取指定注解属性，然后将其再进行一层包装
	 */
	private void processAttribute(
		SynthesizedAnnotationAggregator synthesizedAnnotationAggregator, AnnotationAttribute originalAttribute,
		AnnotationAttribute target, BinaryOperator<AnnotationAttribute> wrapping) {
		Opt.ofNullable(target.getAnnotationType())
			.map(synthesizedAnnotationAggregator::getSynthesizedAnnotation)
			.ifPresent(t -> t.replaceAttribute(target.getAttributeName(), old -> wrapping.apply(old, originalAttribute)));
	}

	/**
	 * 检查两个属性是否互为别名
	 */
	private void checkCircularDependency(AnnotationAttribute original, AnnotationAttribute alias) {
		SyntheticAnnotationUtil.checkLinkedSelf(original, alias);
		Link annotation = SyntheticAnnotationUtil.getLink(alias, RelationType.ALIAS_FOR, RelationType.FORCE_ALIAS_FOR);
		if (ObjectUtil.isNull(annotation)) {
			return;
		}
		final Class<?> aliasAnnotationType = SyntheticAnnotationUtil.getLinkedAnnotationType(annotation, alias.getAnnotationType());
		if (ObjectUtil.notEqual(aliasAnnotationType, original.getAnnotationType())) {
			return;
		}
		Assert.notEquals(
			annotation.attribute(), original.getAttributeName(),
			"circular reference between the alias attribute [{}] and the original attribute [{}]",
			alias.getAttribute(), original.getAttribute()
		);
	}

}

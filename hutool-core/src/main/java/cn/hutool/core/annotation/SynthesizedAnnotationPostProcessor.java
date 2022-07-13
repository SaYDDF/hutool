package cn.hutool.core.annotation;

import cn.hutool.core.comparator.CompareUtil;

import java.util.Comparator;

/**
 * <p>被合成注解后置处理器，用于在{@link SynthesizedAnnotationAggregator}加载完所有待合成注解后，
 * 再对加载好的{@link SynthesizedAnnotation}进行后置处理。<br>
 * 当多个{@link SynthesizedAnnotationPostProcessor}需要一起执行时，将按照{@link #order()}的返回值进行排序，
 * 该值更小的处理器将被优先执行。
 *
 * <p>该接口存在多个实现类，调用者应当保证在任何时候，对一批后置处理器的调用顺序都符合：
 * <ul>
 *     <li>{@link AliasAttributePostProcessor}；</li>
 *     <li>{@link MirrorLinkAttributePostProcessor}；</li>
 *     <li>{@link AliasForLinkAttributePostProcessor}；</li>
 *     <li>其他后置处理器；</li>
 * </ul>
 *
 * @author huangchengxing
 * @see AliasAttributePostProcessor
 * @see MirrorLinkAttributePostProcessor
 * @see AliasForLinkAttributePostProcessor
 */
public interface SynthesizedAnnotationPostProcessor extends Comparable<SynthesizedAnnotationPostProcessor> {

	/**
	 * 在一组后置处理器中被调用的顺序，越小越靠前
	 *
	 * @return 排序值
	 */
	default int order() {
		return Integer.MAX_VALUE;
	}

	/**
	 * 比较两个后置处理器的{@link #order()}返回值
	 *
	 * @param o 比较对象
	 * @return 大小
	 */
	@Override
	default int compareTo(SynthesizedAnnotationPostProcessor o) {
		return CompareUtil.compare(this, o, Comparator.comparing(SynthesizedAnnotationPostProcessor::order));
	}

	/**
	 * 给定指定被合成注解与其所属的合成注解实例，经过处理后返回最终
	 *
	 * @param annotation          被合成的注解
	 * @param synthesizedAnnotationAggregator 注解所属的合成注解
	 */
	void process(SynthesizedAnnotation annotation, SynthesizedAnnotationAggregator synthesizedAnnotationAggregator);

}

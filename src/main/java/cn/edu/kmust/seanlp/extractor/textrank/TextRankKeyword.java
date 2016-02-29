package cn.edu.kmust.seanlp.extractor.textrank;

import java.util.*;

import cn.edu.kmust.seanlp.Config;
import cn.edu.kmust.seanlp.algoritm.MaxHeap;
import cn.edu.kmust.seanlp.extractor.domain.KeyTerm;
import cn.edu.kmust.seanlp.segmenter.domain.Term;

/**
 * 基于TextRank算法的关键字提取，适用于单文档
 * 源自hankcs的对中文的实现
 * @author  Zhao Shiyu
 *
 */
public class TextRankKeyword extends AbstractTextRank {
	/**
	 * 提取多少个关键字
	 */
	int nKeyword = 10;
	
	/**
	 * 提取关键词
	 * 
	 * @param document
	 *            文档内容
	 * @param size
	 *            希望提取几个关键词
	 * @return 一个列表
	 */
	public static List<KeyTerm> getKeywordList(String document, int size) {
		TextRankKeyword textRankKeyword = new TextRankKeyword();
		textRankKeyword.nKeyword = size;

		return textRankKeyword.getKeyword(document);
	}

	/**
	 * 提取关键词
	 * 
	 * @param content
	 * @return
	 */
	public List<KeyTerm> getKeyword(String content) {
		Set<Map.Entry<String, Float>> entrySet = getTermAndRank(content, nKeyword).entrySet();
		List<KeyTerm> result = new ArrayList<KeyTerm>(entrySet.size());
		for (Map.Entry<String, Float> entry : entrySet) {
			result.add(new KeyTerm(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	/**
	 * 返回全部分词结果和对应的rank
	 * 
	 * @param content
	 * @return
	 */
	public Map<String, Float> getTermAndRank(String content) {
		assert content != null;
		List<Term> termList = defaultSegment.segment(content);
		if (Config.DEBUG) {
			System.out.println("分词结果：" + termList);
		}
		return getRank(termList);
	}

	/**
	 * 返回分数最高的前size个分词结果和对应的rank
	 * 
	 * @param content
	 * @param size
	 * @return
	 */
	public Map<String, Float> getTermAndRank(String content, Integer size) {
		Map<String, Float> map = getTermAndRank(content);
		Map<String, Float> result = new LinkedHashMap<String, Float>();
		for (Map.Entry<String, Float> entry : new MaxHeap<Map.Entry<String, Float>>(
				size, new Comparator<Map.Entry<String, Float>>() {
					@Override
					public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}
				}).addAll(map.entrySet()).toList()) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	/**
	 * 使用已经分好的词来计算rank
	 * 
	 * @param termList
	 * @return
	 */
	public Map<String, Float> getRank(List<Term> termList) {
		List<String> wordList = new ArrayList<String>(termList.size());
		for (Term term: termList) {
			if (shouldInclude(term)) {
				wordList.add(term.getWord());
			}
		}
		// System.out.println(wordList);
		Map<String, Set<String>> words = new TreeMap<String, Set<String>>();
		Queue<String> que = new LinkedList<String>();
		for (String w : wordList) {
			if (!words.containsKey(w)) {
				words.put(w, new TreeSet<String>());
			}
			que.offer(w);
			if (que.size() > 5) {
				que.poll();
			}

			for (String w1 : que) {
				for (String w2 : que) {
					if (w1.equals(w2)) {
						continue;
					}

					words.get(w1).add(w2);
					words.get(w2).add(w1);
				}
			}
		}
		// System.out.println(words);
		Map<String, Float> score = new HashMap<String, Float>();
		for (int i = 0; i < max_iter; ++i) {
			Map<String, Float> m = new HashMap<String, Float>();
			float max_diff = 0;
			for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
				String key = entry.getKey();
				Set<String> value = entry.getValue();
				m.put(key, 1 - d);
				for (String element : value) {
					int size = words.get(element).size();
					if (key.equals(element) || size == 0)
						continue;
					m.put(key,
							m.get(key)
									+ d
									/ size
									* (score.get(element) == null ? 0 : score
											.get(element)));
				}
				max_diff = Math.max(max_diff, Math.abs(m.get(key)
						- (score.get(key) == null ? 0 : score.get(key))));
			}
			score = m;
			if (max_diff <= min_diff)
				break;
		}

		return score;
	}
}
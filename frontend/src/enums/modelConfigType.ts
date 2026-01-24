export enum ModelConfigType {
  EMBEDDING = 'embedding',
  LLM = 'llm'
}

export const ModelConfigTypeLabels = {
  [ModelConfigType.EMBEDDING]: '向量模型',
  [ModelConfigType.LLM]: '大语言模型'
}

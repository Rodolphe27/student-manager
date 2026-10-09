import Anthropic from '@anthropic-ai/sdk';
import { Injectable, Logger } from '@nestjs/common';
import { ApiError } from '../common/errors';
import { config } from '../config';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ToolSpec {
  name: string;
  description: string;
  properties: Record<string, Record<string, unknown>>;
  required: string[];
}

export interface Toolbox {
  specs(): ToolSpec[];
  run(name: string, input: Record<string, unknown>): Promise<string>;
}

export const MAX_STEPS = 6;
const MAX_TOKENS = 4096;

/** Talks to Claude. Replaced by a fake in tests; the API key never leaves the server. */
export abstract class AssistantClient {
  abstract isConfigured(): boolean;
  abstract reply(systemPrompt: string, history: ChatMessage[], tools: Toolbox): Promise<string>;
}

@Injectable()
export class ClaudeAssistantClient extends AssistantClient {
  private readonly log = new Logger('Claude');
  private client: Anthropic | null = null;
  private readonly apiKey = config().anthropicApiKey;
  private readonly model = config().chatModel;

  isConfigured(): boolean {
    return this.apiKey.trim() !== '';
  }

  private api(): Anthropic {
    if (!this.client) this.client = new Anthropic({ apiKey: this.apiKey, timeout: 90_000, maxRetries: 1 });
    return this.client;
  }

  async reply(systemPrompt: string, history: ChatMessage[], tools: Toolbox): Promise<string> {
    if (!this.isConfigured()) throw new ApiError(503, 'The assistant is not set up on this server.');
    const toolDefs = tools.specs().map((spec) => ({
      name: spec.name,
      description: spec.description,
      input_schema: { type: 'object' as const, properties: spec.properties, required: spec.required },
    }));
    const messages: Anthropic.MessageParam[] = history.map((m) => ({ role: m.role, content: m.content }));
    try {
      for (let step = 0; step < MAX_STEPS; step++) {
        const response = await this.api().messages.create({
          model: this.model,
          max_tokens: MAX_TOKENS,
          system: systemPrompt,
          tools: toolDefs,
          messages,
        });
        // The whole content (incl. any thinking blocks) goes back unchanged, as the API requires.
        messages.push({ role: 'assistant', content: response.content });
        if (response.stop_reason !== 'tool_use') return textOf(response);
        const results: Anthropic.ToolResultBlockParam[] = [];
        for (const block of response.content) {
          if (block.type !== 'tool_use') continue;
          results.push({
            type: 'tool_result',
            tool_use_id: block.id,
            content: await this.runTool(tools, block.name, block.input),
          });
        }
        messages.push({ role: 'user', content: results });
      }
    } catch (error) {
      this.log.warn(`Claude request failed: ${(error as Error).message}`);
      throw new ApiError(503, 'The assistant could not answer right now. Please try again.');
    }
    return 'I could not finish that request. Please try asking in a simpler way.';
  }

  private async runTool(tools: Toolbox, name: string, input: unknown): Promise<string> {
    const args = input && typeof input === 'object' && !Array.isArray(input) ? (input as Record<string, unknown>) : {};
    try {
      return await tools.run(name, args);
    } catch (error) {
      this.log.warn(`Assistant tool ${name} failed: ${error}`);
      return 'Error: the tool failed.';
    }
  }
}

function textOf(response: Anthropic.Message): string {
  const text = response.content
    .flatMap((b) => (b.type === 'text' ? [b.text] : []))
    .join('\n')
    .trim();
  return text === '' ? 'I do not have an answer for that.' : text;
}

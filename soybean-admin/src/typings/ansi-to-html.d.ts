declare module 'ansi-to-html' {
  interface ConverterOptions {
    fg?: string;
    bg?: string;
    escapeXML?: boolean;
  }

  class Convert {
    constructor(options?: ConverterOptions);
    toHtml(value: string): string;
  }

  export default Convert;
}

import { useMemo } from "react";
import CodeMirror from "@uiw/react-codemirror";
import { cpp } from "@codemirror/lang-cpp";
import { python } from "@codemirror/lang-python";
import { oneDark } from "@codemirror/theme-one-dark";

type CodeEditorProps = {
  languageId: number;
  sourceCode: string;
  onChange: (value: string) => void;
};

export default function CodeEditor({ languageId, sourceCode, onChange }: CodeEditorProps) {
  const extensions = useMemo(() => (languageId === 54 ? [cpp()] : [python()]), [languageId]);
  const theme = document.documentElement.dataset.theme === "dark" ? oneDark : undefined;

  return (
    <CodeMirror
      value={sourceCode}
      minHeight="440px"
      extensions={extensions}
      theme={theme}
      onChange={onChange}
      basicSetup={{
        lineNumbers: true,
        foldGutter: true,
        highlightActiveLine: true
      }}
    />
  );
}

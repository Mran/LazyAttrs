import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mran on 2017/3/19.
 */
public class LazyAttrs extends AnAction {
    String a = "attrs";
    List<ElementWrapper> elementWrappers;
    String styleableName;

    //入口
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        styleableName = getSelectWord(e);
        XmlFile file;
        if (!TextUtils.isEmpty(styleableName)) {
            file = getFile(e.getProject());
            if (file != null) {
                elementWrappers = getAttrs(file);
                if (elementWrappers != null)
                    writeToClass(e);
            }
        }
    }


    //获取选择的文字
    private String getSelectWord(AnActionEvent e) {
        Editor mEditor = e.getData(PlatformDataKeys.EDITOR);
        if (null != mEditor) {

            SelectionModel model = mEditor.getSelectionModel();
            final String selectedText = model.getSelectedText();
            if (!TextUtils.isEmpty(selectedText)) {
                return selectedText;
            }
        }
        return "";

    }

    //获取attr文件
    private XmlFile getFile(Project project) {
        PsiFile[] mPsiFiles = FilenameIndex.getFilesByName(project, "attrs.xml", GlobalSearchScope.projectScope(project));
        if (mPsiFiles.length <= 0) {
            return null;
        }
        return (XmlFile) mPsiFiles[0];
    }

    //写入代码
    private void writeToClass(AnActionEvent e) {
        final Project project = e.getProject();
        final PsiClass psiClass;
        psiClass = getWriteClass(e);
        if (psiClass != null)
            new WriteCommandAction.Simple(project) {
                @Override
                protected void run() throws Throwable {

                    write(psiClass, project);
                }
            }.execute();


    }

    //对文件进行解析
    private List<ElementWrapper> getAttrs(XmlFile xmlFile) {

        if (xmlFile.getRootTag() == null) {
            return null;
        }
        XmlTag xmlTags[] = xmlFile.getRootTag().getSubTags();
        List<ElementWrapper> elementWrappers = new ArrayList<ElementWrapper>();
        for (XmlTag x1 : xmlTags) {
            //解析到 <declare-styleable name="TopBar">
            ElementWrapper elementWrapper = new ElementWrapper();
            //解析style名
            elementWrapper.setStyleName(x1.getAttributeValue("name"));
            XmlTag xmlTag2[] = x1.getSubTags();
            List<MyElement> elements = new ArrayList<MyElement>();
            //解析到  <attr name="title" format="string"/>
            for (XmlTag x2 : xmlTag2) {
                MyElement myElement = new MyElement();
                //解析配置名和配置对应的数据格式
                myElement.setName(x2.getAttributeValue("name"));
                myElement.setFormat(x2.getAttributeValue("format"));
                elements.add(myElement);
            }
            elementWrapper.setElements(elements);
            elementWrappers.add(elementWrapper);
        }
        return elementWrappers;
    }

    //获得要写入的类
    private PsiClass getWriteClass(AnActionEvent event) {
        final Project project = event.getProject();
        PsiFile psiFile;
        PsiClass psiClass = null;
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if (project != null) {

            Document document;
            if (editor != null) {
                document = editor.getDocument();
                psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if ((psiFile) != null) {
                    psiClass = ((PsiJavaFile) psiFile).getClasses()[0];
                }
            }
        }
        return psiClass;
    }

    //生成需要写入的语句
    private String getStatement(MyElement m) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(m.getName()).append("= typedArray.");
        String type;
        String end = "";

        switch (m.getFormat()) {
            case Costant.STRING:
                type = "getString(R.styleable.";
                break;
            case Costant.BOOL:
                type = "getBoolean(R.styleable.";
                end = ",false";
                break;
            case Costant.COLOR:
                type = "getColor(R.styleable.";
                end = ",0xffffff";
                break;
            case Costant.DIMENSION:
                type = "getDimension(R.styleable.";
                end = ",10.0";
                break;
            case Costant.INT:
                type = "getInteger(R.styleable.";
                end = ",1";
                break;
            case Costant.FLOAT:
                type = "getFloat(R.styleable.";
                end = ",1.0";
                break;
            default:
                type = "";
                break;
        }
        if (!type.equals(""))
            stringBuilder.append(type).append(styleableName).append("_").append(m.getName()).append(end).append(");\n");

        return stringBuilder.toString();
    }

    //写入变量
    private void writeField(PsiClass psiClass, PsiElementFactory psiElementFactory) {
        //写入生成的变量
        for (ElementWrapper e : elementWrappers) {
            if (e.getStyleName().equals(styleableName))
                for (MyElement m : e.getElements()) {
                    String type = "";
                    switch (m.getFormat()) {
                        case Costant.STRING:
                            type = "String ";
                            break;
                        case Costant.BOOL:
                            type = "boolean ";
                            break;
                        case Costant.COLOR:
                            type = "int ";
                            break;
                        case Costant.DIMENSION:
                            type = "float ";
                            break;
                    }
                    psiClass.add(psiElementFactory.createFieldFromText(type + m.getName() + ";\n", psiClass));
                }
        }
    }

    //写入方法
    private void writeMethod(PsiClass psiClass, PsiElementFactory psiElementFactory) {
        //找到要写入的方法.
        PsiMethod psiMethod[] = psiClass.findMethodsByName("getAttrs", false);
        PsiMethod psiMethod1;
        //不存在就创建一个.
        if (psiMethod.length == 0) {
            psiMethod1 = (PsiMethod) psiClass.add(psiElementFactory.createMethod("getAttrs", PsiType.VOID));
        } else
            psiMethod1 = psiMethod[0];
        //写入代码
        for (ElementWrapper e : elementWrappers) {
            if (e.getStyleName().equals(styleableName))
                for (MyElement m : e.getElements()) {
                    psiMethod1.getBody().add(psiElementFactory.createStatementFromText(getStatement(m), psiClass));
                }
        }
    }

    private void write(PsiClass psiClass, Project project) {
        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();

        writeField(psiClass, psiElementFactory);
        writeMethod(psiClass, psiElementFactory);

    }


}

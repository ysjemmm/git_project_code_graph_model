<script setup lang="ts">
import { ref } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

const visible = ref(false)
const currentStep = ref(0)

const steps = [
  { title: '配置二方包规则' },
  { title: '解析 Maven 依赖' },
  { title: '关联二方包项目' },
  { title: '同步到图谱' },
]
</script>

<template>
  <span>
    <a-button type="link" size="small" style="padding: 0 4px; font-size: 12px;" @click.stop="visible = true">
      <template #icon><QuestionCircleOutlined /></template>
      新手引导
    </a-button>

    <a-modal
      v-model:open="visible"
      title="Maven 依赖功能使用指南"
      :footer="null"
      width="680px"
      @cancel="visible = false"
    >
      <a-steps
        :current="currentStep"
        size="small"
        style="margin-bottom: 24px;"
        @change="(v: number) => currentStep = v"
      >
        <a-step v-for="s in steps" :key="s.title" :title="s.title" />
      </a-steps>

      <!-- Step 0 -->
      <div v-if="currentStep === 0">
        <a-alert
          type="info"
          show-icon
          message="第一步：告诉系统哪些包是「二方包」"
          style="margin-bottom: 16px;"
        />
        <a-timeline>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">进入「二方包规则」页面</div>
              <div class="guide-desc">左侧菜单 → 二方包规则</div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">点击「新增规则」</div>
              <div class="guide-desc">
                填写规则名称（如：公司内部包），然后填写 groupId 关键词。<br/>
                例如公司所有包的 groupId 都以 <code>com.example</code> 开头，就填 <code>com.example</code>，模式选「文本」。<br/>
                artifactId 如果不限制，切换到「正则」模式填 <code>.*</code>（匹配所有包名）。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">保存并确认规则已启用</div>
              <div class="guide-desc">表格里的开关显示「启用」状态即可。可以配置多条规则，数字越小优先级越高。</div>
            </div>
          </a-timeline-item>
        </a-timeline>
        <div class="guide-tip">
          💡 二方包 = 本公司/本团队自己开发的包；三方包 = 开源或外部采购的包（如 Spring、Hutool）。
        </div>
      </div>

      <!-- Step 1 -->
      <div v-if="currentStep === 1">
        <a-alert
          type="info"
          show-icon
          message="第二步：解析项目的 Maven 依赖"
          style="margin-bottom: 16px;"
        />
        <a-timeline>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">进入「应用管理」，点击应用名称</div>
              <div class="guide-desc">左侧菜单 → 应用管理 → 点击蓝色的应用名称进入详情页</div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">展开「Maven 依赖」面板</div>
              <div class="guide-desc">点击面板标题展开，默认是折叠状态。</div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">点击「解析依赖」按钮</div>
              <div class="guide-desc">
                系统会读取本地缓存的 <code>pom.xml</code> 文件，解析所有依赖并按二方包规则自动分类。<br/>
                <strong>前提：</strong>该应用的 Git 仓库已经缓存到本地（「Git 缓存目录」显示绿色「已存在」）。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">查看解析结果</div>
              <div class="guide-desc">
                蓝色「二方包」标签 = 匹配了你配置的规则；灰色「三方包」= 未匹配。<br/>
                点击蓝色「二方包」标签可以查看是哪条规则匹配的。<br/>
                如果某些依赖版本显示为空，说明版本定义在外部父 BOM 里，本地无法解析，属于正常现象。
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
        <div class="guide-tip">
          💡 如果修改了二方包规则，需要重新点「解析依赖」才能刷新分类结果。
        </div>
      </div>

      <!-- Step 2 -->
      <div v-if="currentStep === 2">
        <a-alert
          type="info"
          show-icon
          message="第三步：把二方包和已导入的项目关联起来"
          style="margin-bottom: 16px;"
        />
        <a-timeline>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">确保被依赖的项目也已导入</div>
              <div class="guide-desc">
                例如项目 A 依赖了项目 B 的包，需要先在「应用管理」里把项目 B 也添加进来，并导入图谱。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">点击二方包行的「关联」按钮</div>
              <div class="guide-desc">
                在依赖表格的「关联项目」列，点击蓝色「二方包」行右侧的「关联」按钮。<br/>
                在弹窗里选择对应的已导入项目，点保存。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">关联成功后「关联项目」列显示项目名</div>
              <div class="guide-desc">绿色标签显示关联的项目名称，点击可以修改，右侧红色按钮可以删除关联。</div>
            </div>
          </a-timeline-item>
        </a-timeline>
        <div class="guide-tip">
          💡 关联的意义：让 AI 在排查问题时，能从项目 A 顺藤摸瓜找到项目 B 的具体代码节点。
        </div>
      </div>

      <!-- Step 3 -->
      <div v-if="currentStep === 3">
        <a-alert
          type="info"
          show-icon
          message="第四步：同步关联关系到代码图谱"
          style="margin-bottom: 16px;"
        />
        <a-timeline>
          <a-timeline-item color="green">
            <div class="guide-item">
              <div class="guide-title">关联时自动同步（推荐）</div>
              <div class="guide-desc">
                每次点「保存」关联时，系统会自动在图谱里创建一条 <code>DEPENDS_ON</code> 边，连接两个项目节点。无需手动操作。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="blue">
            <div class="guide-item">
              <div class="guide-title">手动批量同步（可选）</div>
              <div class="guide-desc">
                点击「同步关联到图谱」按钮，可以把所有已保存的关联一次性同步到 Neo4j 图谱。<br/>
                适用于图谱重建后需要重新写入关联边的场景。
              </div>
            </div>
          </a-timeline-item>
          <a-timeline-item color="green">
            <div class="guide-item">
              <div class="guide-title">完成！AI 现在可以跨项目排查问题了</div>
              <div class="guide-desc">
                当 AI 在分析项目 A 时发现问题可能在二方包 B，它会通过 <code>DEPENDS_ON</code> 边找到项目 B 的代码节点，继续深入排查。
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
        <div class="guide-tip">
          💡 删除关联时，图谱里对应的 DEPENDS_ON 边也会自动删除。
        </div>
      </div>

      <!-- 底部导航 -->
      <div class="guide-footer">
        <a-button :disabled="currentStep === 0" @click="currentStep--">上一步</a-button>
        <a-button
          v-if="currentStep < steps.length - 1"
          type="primary"
          @click="currentStep++"
        >下一步</a-button>
        <a-button v-else type="primary" @click="visible = false">完成</a-button>
      </div>
    </a-modal>
  </span>
</template>

<style scoped>
.guide-item {
  padding-bottom: 4px;
}

.guide-title {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 4px;
}

.guide-desc {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.7;
}

.guide-tip {
  margin-top: 16px;
  padding: 10px 14px;
  background: rgba(22, 119, 255, 0.04);
  border: 1px solid rgba(22, 119, 255, 0.15);
  border-radius: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.6;
}

.guide-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

code {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 5px;
  border-radius: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}
</style>

package autonavi.online.framework.configcenter.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import autonavi.online.framework.cc.CcBaseEntity;
import autonavi.online.framework.cc.Miscellaneous;
import autonavi.online.framework.configcenter.commons.AppNode;
import autonavi.online.framework.configcenter.entity.ResultEntity;
import autonavi.online.framework.configcenter.exception.AofException;
import autonavi.online.framework.configcenter.service.ZookeeperService;
import autonavi.online.framework.configcenter.util.AofCcProps;
import autonavi.online.framework.util.json.JsonBinder;
import autonavi.online.framework.zookeeper.SysProps;

@Controller
public class ConfigCenterForRunController {
	static{
		Miscellaneous.setNodeIndex(32);
	}
	
	private Logger logger = LogManager.getLogger(getClass());
	
	@Resource private ZookeeperService zooKeeperService;
	/**
	 * 初始化运行模式 读出所有的配置中的临时配置
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/index_run")
	public @ResponseBody Object initIndexRun(HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("初始化运行模式编辑中的临时配置  应用名称["+app+"]");
		ResultEntity entity=new ResultEntity();
		Map<String,List<Map<String,String>>> object=new HashMap<String,List<Map<String,String>>>();
		try {
			//获取临时路径下所有子节点
			List<String> list=zooKeeperService.getAppNodeTree(SysProps.AOF_TEMP_ROOT+"/"+app, zk);
			List<Map<String,String>> listBase=new ArrayList<Map<String,String>>();
			List<Map<String,String>> listBiz=new ArrayList<Map<String,String>>();
			for(String node:list){
				//base
				if(("/"+node).startsWith(SysProps.AOF_APP_BASE)){
					Map<String,String> map=new HashMap<String,String>();
					map.put("baseName", node);
					map.put("basePath",node);
					map.put("lastModify",DateFormatUtils.format(new Date(zooKeeperService.getAppNodeStat(SysProps.AOF_TEMP_ROOT+"/"+app+"/"+node, zk).getCtime()), "yyyy-MM-dd HH:mm:ss"));
					listBase.add(map);
				}else if(("/"+node).startsWith(SysProps.AOF_APP_BIZ)){
					Map<String,String> map=new HashMap<String,String>();
					map.put("bizName", node);
					map.put("bizPath", node);
					map.put("lastModify",DateFormatUtils.format(new Date(zooKeeperService.getAppNodeStat(SysProps.AOF_TEMP_ROOT+"/"+app+"/"+node, zk).getCtime()), "yyyy-MM-dd HH:mm:ss"));
					listBiz.add(map);
				}
			}
			object.put("base", listBase);
			object.put("biz", listBiz);
			
			
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(object);
			
			
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	/**
	 * 初始化base信息编辑
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/manager/base_edit_run")
	public @ResponseBody Object baseEditRun(@RequestParam String fileName,HttpServletRequest request, HttpServletRequest response){
		logger.info("获取base数据源列表");
		String app=(String)request.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)request.getSession().getAttribute(AofCcProps.SESSION_ZK);
		ResultEntity entity = new ResultEntity();
		try {
			CcBaseEntity ccBase = zooKeeperService.getCcBaseEntity(zk, app, fileName);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(ccBase);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		return entity;
	}
	/**
	 * 初始化sharding信息
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/manager/base_edit_run_shard")
	public @ResponseBody Object baseEditRunShard(HttpServletRequest request, HttpServletRequest response){
		logger.info("获取Sharding数据源列表");
		String app=(String)request.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)request.getSession().getAttribute(AofCcProps.SESSION_ZK);
		ResultEntity entity = new ResultEntity();
		try {
			CcBaseEntity ccBase = zooKeeperService.getCcShardingEntity(zk, app);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(ccBase);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		return entity;
	}
	/**
	 * 导入BASE信息
	 * @param imports
	 * @param res
	 * @return
	 */
	
	@RequestMapping("/manager/import_dss_config")
	public @ResponseBody Object importDssConfig(@RequestParam String imports, HttpServletRequest res) {
		ResultEntity entity = new ResultEntity();
		CcBaseEntity ccBaseEntity=null;
		try {
			ccBaseEntity = JsonBinder.buildNonDefaultBinder().fromJson(imports, CcBaseEntity.class);
			
		} catch (Exception e1) {
			entity.setCode("1");
			entity.setMsg("JSON格式错误");
			return entity;
		}
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("导入BASE到正式配置 应用名称["+app+"]");
		
		try {
			zooKeeperService.importDssConfig(ccBaseEntity, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBaseEntity);
		return entity;
	}
	/**
	 * 导入BIZ信息
	 * @param ccBase
	 * @param res
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("/manager/import_biz_config")
	public @ResponseBody Object importBizConfig(@RequestParam String imports, HttpServletRequest res) {
		ResultEntity entity = new ResultEntity();
		Map<String,String> ccBase=new HashMap<String,String>();
		try{
			JsonBinder builder=JsonBinder.buildNonDefaultBinder();
			ccBase=builder.fromJson(imports, HashMap.class, builder.getCollectionType(HashMap.class, String.class,String.class));
		}catch(Exception e){
			entity.setCode("1");
			entity.setMsg("JSON格式错误");
			return entity;
		}
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("导入BIZ配置 应用名称["+app+"]");
		
		try {
			zooKeeperService.importBizConfig(ccBase, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBase);
		return entity;
	}
	
	/**
	 * 把页面上的内容保存到zooKeeper
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/base_edit_run_dss_save")
	public @ResponseBody Object baseEditRunDssSave(@RequestBody CcBaseEntity ccBaseEntity, HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("存储新的正式配置 应用名称["+app+"]");
		ResultEntity entity = new ResultEntity();
		try {
			zooKeeperService.saveBaseConfig(ccBaseEntity, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBaseEntity);
		return entity;
	}
	/**
	 * 把页面上的内容保存到zooKeeper
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/base_edit_run_shard_save")
	public @ResponseBody Object baseEditRunShardSave(@RequestBody CcBaseEntity ccBaseEntity, HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("存储新的正式分区分表配置 应用名称["+app+"]");
		ResultEntity entity = new ResultEntity();
		try {
			zooKeeperService.saveShardingConfig(ccBaseEntity, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBaseEntity);
		return entity;
	}
	/**
	 * 把页面上的内容保存到zooKeeper 临时目录
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/base_edit_run_save/{fileName}")
	public @ResponseBody Object baseEditRunSave(@RequestBody CcBaseEntity ccBaseEntity, @PathVariable("fileName") String fileName,HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		//String fileName=(String)res.getSession().getAttribute(AofCcProps.TEMPNAME);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("存储新的临时配置 应用名称["+app+"] 配置名称["+fileName+"]");
		ResultEntity entity = new ResultEntity();
		try {
			zooKeeperService.saveBaseConfigToTemp(ccBaseEntity, SysProps.AOF_APP_BASE.replaceAll("/", "")+"_"+fileName, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBaseEntity);
		return entity;
	}
	/**
	 * 校验文件是否存在
	 * @param fileName
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/edit_run_check_file")
	public @ResponseBody Object editDevCheckFile(@RequestParam String fileName,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("校验新的临时配置 应用名称["+app+"] 配置名称["+fileName+"]");
		ResultEntity entity=new ResultEntity();
		if(zooKeeperService.getAppNodeStat(SysProps.AOF_TEMP_ROOT+"/"+app+SysProps.AOF_APP_BASE+"_"+fileName, zk)!=null){
			entity.setCode("1");
			entity.setMsg("配置已经存在");
		}else{
			//res.getSession().setAttribute(AofCcProps.TEMPNAME, fileName);
			entity.setCode("0");
			entity.setMsg("success");
		}
		return entity;
	}
	/**
	 * 初始化检测服务器状态
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/init_check_appNode")
	public @ResponseBody Object initAppNodeStat(HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("检查各个节点心跳状态 应用名称["+app+"]");
		ResultEntity entity=new ResultEntity();
		try {
			List<AppNode> l=zooKeeperService.checkAppNodesStats(zk, app);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(l);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	@RequestMapping("/manager/del_app_node")
	public @ResponseBody Object deleteAppNode(@RequestParam String nodeName,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("删除节点 应用名称["+app+"] 节点名称["+nodeName+"]");
		ResultEntity entity=new ResultEntity();
		try {
			zooKeeperService.deleteServerNode(zk, app, nodeName);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	@RequestMapping("/manager/del_temp_config")
	public @ResponseBody Object deleteTempConfig(@RequestParam String nodeName,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("删除临时配置 应用名称["+app+"] 配置名称["+nodeName+"]");
		ResultEntity entity=new ResultEntity();
		try {
			zooKeeperService.deleteTempConfig(zk, app, nodeName);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	/**
	 * 预激活数据源
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/pre_active_dss")
	public @ResponseBody Object preActiveDss(@RequestBody List<AppNode> nodeList , HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("预激活数据源配置 应用名称["+app+"] ");
		ResultEntity entity = new ResultEntity();
		try {
			String version=zooKeeperService.preBaseActive(zk, app, nodeList);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(version);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
		
	}
	/**
	 * 预激活数据源
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/pre_active_biz")
	public @ResponseBody Object preActiveBiz(@RequestBody List<AppNode> nodeList ,HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("预激活数据源配置 应用名称["+app+"] ");
		ResultEntity entity = new ResultEntity();
		try {
			String version=zooKeeperService.preBizActive(zk, app, nodeList);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(version);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
		
	}
	/**
	 * 初始化检测服务器状态
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/init_commit_appNode")
	public @ResponseBody Object initCommitAppNode(@RequestBody List<AppNode> nodeList,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("检查各个节点预提交状态 应用名称["+app+"]");
		ResultEntity entity=new ResultEntity();
		try {
			List<AppNode> l=zooKeeperService.checkPreBaseActive(zk, app, nodeList);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(l);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	/**
	 * 激活数据源
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/commit_active_dss/{tempName}")
	public @ResponseBody Object commitActiveDss(@RequestBody List<AppNode> nodeList ,@PathVariable("tempName") String tempName, HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		logger.info("激活数据源配置 应用名称["+app+"] ");
		ResultEntity entity = new ResultEntity();
		try {
			String version=zooKeeperService.commitBaseActive(zk, app, nodeList,tempName,pass);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(version);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
		
	}
	/**
	 * 激活Biz
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/commit_active_biz/{tempName}")
	public @ResponseBody Object commitActiveBiz(@RequestBody List<AppNode> nodeList, @PathVariable("tempName") String tempName , HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("激活数据源配置 应用名称["+app+"] ");
		ResultEntity entity = new ResultEntity();
		try {
			//zooKeeperService.copyAppNode(SysProps.AOF_TEMP_ROOT+"/"+app+"/"+tempName, SysProps.AOF_ROOT+"/"+app+SysProps.AOF_APP_BIZ, zk, app, pass);
			String version=zooKeeperService.commitBizActive(zk, app, nodeList,tempName,pass);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(version);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
		
	}
	/**
	 * 初始化检测服务器状态
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/check_commit_appNode")
	public @ResponseBody Object checkCommitAppNode(@RequestBody List<AppNode> nodeList,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("检查各个节点的提交状态 应用名称["+app+"]");
		ResultEntity entity=new ResultEntity();
		try {
			List<AppNode> l=zooKeeperService.checkCommitBaseActive(zk, app, nodeList);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(l);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		return entity;
	}
	/**
	 * 初始化配置信息
	 * @param fileName
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/biz_edit_run")
	public @ResponseBody Object bizEditRun(@RequestParam String fileName,HttpServletRequest request){
		logger.info("获取自定义配置信息-热部署模式");
		String app=(String)request.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)request.getSession().getAttribute(AofCcProps.SESSION_ZK);
		ResultEntity entity = new ResultEntity();
		try {
			Map<String,String> ccBase = zooKeeperService.getBizInfo(zk, app,fileName);
			entity.setCode("0");
			entity.setMsg("success");
			entity.setResult(ccBase);
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		return entity;
	}
	/**
	 * 校验BIZ名称
	 * @param fileName
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/edit_run_check_biz_file")
	public @ResponseBody Object editDevCheckBizFile(@RequestParam String fileName,HttpServletRequest res){
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("校验新的临时配置 应用名称["+app+"] 配置名称["+fileName+"]");
		ResultEntity entity=new ResultEntity();
		if(zooKeeperService.getAppNodeStat(SysProps.AOF_TEMP_ROOT+"/"+app+SysProps.AOF_APP_BIZ+"_"+fileName, zk)!=null){
			entity.setCode("1");
			entity.setMsg("配置已经存在");
		}else{
			//res.getSession().setAttribute(AofCcProps.TEMPNAME, fileName);
			entity.setCode("0");
			entity.setMsg("success");
		}
		return entity;
	}
	/**
	 * 把页面上的内容保存到zooKeeper 临时目录
	 * @param ccBaseEntity
	 * @param res
	 * @return
	 */
	@RequestMapping("/manager/biz_edit_run_save/{fileName}")
	public @ResponseBody Object bizEditRunSave(@RequestBody Map<String,String> ccBase,@PathVariable("fileName") String fileName, HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
//		String fileName=(String)res.getSession().getAttribute(AofCcProps.TEMPNAME);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("存储新的临时配置 应用名称["+app+"] 配置名称["+fileName+"]");
		ResultEntity entity = new ResultEntity();
		try {
			zooKeeperService.saveBizConfigToTemp(ccBase, SysProps.AOF_APP_BIZ.replaceAll("/", "")+"_"+fileName, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBase);
		return entity;
	}
	@RequestMapping("/manager/biz_edit_run_cold_save")
	public @ResponseBody Object bizEditRunColdSave(@RequestBody Map<String,String> ccBase, HttpServletRequest res) {
		String app=(String)res.getSession().getAttribute(AofCcProps.SESSION_APP);
		String pass=(String)res.getSession().getAttribute(AofCcProps.SESSION_PASS);
		ZooKeeper zk=(ZooKeeper)res.getSession().getAttribute(AofCcProps.SESSION_ZK);
		logger.info("存储新的配置 应用名称["+app+"]");
		ResultEntity entity = new ResultEntity();
		try {
			zooKeeperService.saveBizConfig(ccBase, zk, app, pass);
			entity.setCode("0");
			entity.setMsg("success");
		} catch (AofException e) {
			entity.setCode(e.getErrorCode()+"");
			entity.setMsg(e.getMessage());
		}
		
		entity.setResult(ccBase);
		return entity;
	}

}
